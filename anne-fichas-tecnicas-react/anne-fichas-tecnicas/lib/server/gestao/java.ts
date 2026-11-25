import { cookies } from "next/headers";

/**
 * Chamadas do BFF ao backend Java (Spring). O cookie de sessão "accessToken"
 * do usuário é repassado; erros viram BackendError com a mensagem e o status
 * que o Java devolveu, para a rota responder igual à tela.
 */

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export class BackendError extends Error {
  status: number;
  /** Código do Java nos 401/403 (ex.: ACCESS_TOKEN_EXPIRED, ACCESS_DENIED), repassado à tela. */
  code?: string;
  constructor(message: string, status = 400, code?: string) {
    super(message);
    this.name = "BackendError";
    this.status = status;
    this.code = code;
  }
}

// Fallback para 401/403 sem corpo JSON (o Java hoje responde { message, code }).
function mensagemPadrao(status: number): string {
  if (status === 401) return "Sua sessão expirou. Faça login novamente.";
  if (status === 403) return "Você não tem permissão para esta operação.";
  return `Erro na requisição ao backend (${status})`;
}

async function chamar(path: string, method: string, body?: unknown): Promise<Response> {
  const token = (await cookies()).get("accessToken")?.value;
  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: { "Content-Type": "application/json", Cookie: token ? `accessToken=${token}` : "" },
      body: body === undefined ? undefined : JSON.stringify(body),
      cache: "no-store",
    });
  } catch {
    throw new BackendError("Não foi possível falar com o servidor. Tente de novo em instantes.", 502);
  }
  if (!res.ok) {
    const payload = await res.json().catch(() => null);
    throw new BackendError(payload?.message ?? mensagemPadrao(res.status), res.status, payload?.code);
  }
  return res;
}

/**
 * Corpo JSON da resposta; resposta sem corpo devolve undefined. Não basta olhar
 * o 204: o Java também responde 201 vazio (ex.: registrar contrato).
 */
async function json<T>(path: string, method: string, body?: unknown): Promise<T> {
  const res = await chamar(path, method, body);
  if (res.status === 204) return undefined as T;
  const texto = await res.text();
  return (texto.trim() ? JSON.parse(texto) : undefined) as T;
}

export const java = {
  get: <T>(path: string) => json<T>(path, "GET"),
  post: <T>(path: string, body: unknown = {}) => json<T>(path, "POST", body),
  put: <T>(path: string, body: unknown) => json<T>(path, "PUT", body),
  patch: <T>(path: string, body: unknown) => json<T>(path, "PATCH", body),
  delete: (path: string) => json<void>(path, "DELETE"),
  /** Download binário (planilha). */
  async bytes(path: string): Promise<ArrayBuffer> {
    return (await chamar(path, "GET")).arrayBuffer();
  },
};

/** Query string só com os parâmetros preenchidos. */
export function query(params: Record<string, string | number | boolean | null | undefined>): string {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v !== null && v !== undefined && v !== "") sp.set(k, String(v));
  const s = sp.toString();
  return s ? `?${s}` : "";
}
