/**
 * Cliente HTTP central. Toda chamada ao backend passa por aqui — nenhum
 * componente deve usar `fetch` diretamente, para manter tratamento de erro,
 * timeout, headers e a renovação da sessão consistentes em um único lugar.
 *
 * Renovação da sessão: o access token (cookie httpOnly) dura 20 min. Quando uma
 * requisição volta 401 com code ACCESS_TOKEN_EXPIRED, o cliente chama
 * /api/auth/refresh-token UMA vez (as demais requisições que falharem ao mesmo
 * tempo esperam a mesma Promise), e repete cada requisição original no máximo
 * uma vez. 403 e os outros 401 nunca renovam. Se a renovação for recusada (401),
 * a sessão é encerrada e o usuário vai para /login.
 */

export class ApiError extends Error {
  status: number;
  issues?: unknown;
  code?: string;

  constructor(message: string, status: number, issues?: unknown, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.issues = issues;
    this.code = code;
  }
}

/** Único 401 que se resolve renovando a sessão (contrato do Java e do middleware.ts). */
export const CODIGO_TOKEN_EXPIRADO = "ACCESS_TOKEN_EXPIRED";

const ROTA_RENOVACAO = "/api/auth/refresh-token";
const ROTA_LOGOUT = "/api/auth/logout";
const PAGINA_LOGIN = "/login";
// Login, renovação e logout respondem 401 por conta própria: nunca disparam renovação.
const PREFIXO_AUTH = "/api/auth/";
// Nome da trava entre abas (Web Locks): duas abas nunca rotacionam o mesmo refresh token.
const TRAVA_RENOVACAO = "anne-renovacao-sessao";

// No servidor (Server Components/Route Handlers) chamamos a API interna por caminho relativo;
// no cliente, o browser resolve "/api/..." contra a própria origem — nunca hardcode um host aqui.
const DEFAULT_TIMEOUT_MS = 10_000;

/** renovada: cookies novos; encerrada: refresh recusado (401); indisponivel: rede/5xx, sessão intacta. */
export type ResultadoRenovacao = "renovada" | "encerrada" | "indisponivel";

let renovacaoEmAndamento: Promise<ResultadoRenovacao> | null = null;
// Conta as renovações bem-sucedidas: um 401 de uma requisição enviada antes da última
// renovação só precisa ser repetido (os cookies já são novos), sem renovar de novo.
let geracaoSessao = 0;
let encerramentoEmAndamento: Promise<void> | null = null;

async function comTimeout(path: string, init: RequestInit, timeoutMs: number): Promise<Response> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    // Nunca envie credenciais para origens cruzadas por engano.
    return await fetch(path, { ...init, signal: controller.signal, credentials: "same-origin" });
  } finally {
    clearTimeout(timeout);
  }
}

async function chamarRenovacao(): Promise<ResultadoRenovacao> {
  try {
    const res = await comTimeout(ROTA_RENOVACAO, { method: "POST" }, DEFAULT_TIMEOUT_MS);
    if (res.ok) return "renovada";
    return res.status === 401 ? "encerrada" : "indisponivel";
  } catch {
    return "indisponivel";
  }
}

function comTravaEntreAbas<T>(fn: () => Promise<T>): Promise<T> {
  const locks = typeof navigator !== "undefined" ? navigator.locks : undefined;
  return locks ? locks.request(TRAVA_RENOVACAO, fn) : fn();
}

/**
 * Renova a sessão uma vez só, mesmo com várias chamadas simultâneas: todas recebem a
 * mesma Promise. A chamada de renovação usa fetch direto (não `request`), então nunca
 * dispara a si mesma.
 */
export function renovarSessao(): Promise<ResultadoRenovacao> {
  if (!renovacaoEmAndamento) {
    renovacaoEmAndamento = comTravaEntreAbas(chamarRenovacao)
      .then((resultado) => {
        if (resultado === "renovada") geracaoSessao++;
        return resultado;
      })
      .finally(() => {
        renovacaoEmAndamento = null;
      });
  }
  return renovacaoEmAndamento;
}

/**
 * Sessão sem conserto (renovação recusada ou 401 não renovável): revoga o que restar
 * no servidor, apaga os cookies e vai para /login. Uma vez só, e nunca estando já no
 * /login (evita laço de redirecionamento).
 */
export function encerrarSessao(): Promise<void> {
  if (!encerramentoEmAndamento) {
    encerramentoEmAndamento = (async () => {
      await comTimeout(ROTA_LOGOUT, { method: "POST" }, DEFAULT_TIMEOUT_MS).catch(() => undefined);
      if (typeof window !== "undefined" && window.location.pathname !== PAGINA_LOGIN) {
        window.location.replace(PAGINA_LOGIN);
      }
    })().finally(() => {
      encerramentoEmAndamento = null;
    });
  }
  return encerramentoEmAndamento;
}

async function request<T>(
  path: string,
  options: RequestInit & { timeoutMs?: number } = {},
  repetida = false
): Promise<T> {
  const { timeoutMs = DEFAULT_TIMEOUT_MS, ...init } = options;
  const geracaoNoEnvio = geracaoSessao;

  let res: Response;
  try {
    res = await comTimeout(
      path,
      { ...init, headers: { "Content-Type": "application/json", ...(init.headers ?? {}) } },
      timeoutMs
    );
  } catch (err) {
    if (err instanceof DOMException && err.name === "AbortError") {
      throw new ApiError("Tempo de requisição esgotado. Tente novamente.", 408);
    }
    throw new ApiError("Falha de conexão com o servidor.", 0);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const payload = await res.json().catch(() => ({}));

  if (res.ok) {
    return (payload?.data ?? payload) as T;
  }

  const erro = new ApiError(
    // Rotas locais respondem { error }; os proxies do backend Java, { message }.
    payload?.error ?? payload?.message ?? `Erro na requisição (${res.status})`,
    res.status,
    payload?.issues,
    payload?.code
  );

  if (res.status === 401 && !path.startsWith(PREFIXO_AUTH)) {
    if (erro.code === CODIGO_TOKEN_EXPIRADO && !repetida) {
      const resultado = geracaoSessao !== geracaoNoEnvio ? "renovada" : await renovarSessao();
      if (resultado === "renovada") return request<T>(path, options, true);
      if (resultado === "indisponivel") throw erro;
    }
    // Renovação recusada, 401 não renovável ou 401 mesmo depois de renovar.
    await encerrarSessao();
  }

  throw erro;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: "POST", body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: "PUT", body: JSON.stringify(body) }),
  patch: <T>(path: string, body: unknown) =>
    request<T>(path, { method: "PATCH", body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};
