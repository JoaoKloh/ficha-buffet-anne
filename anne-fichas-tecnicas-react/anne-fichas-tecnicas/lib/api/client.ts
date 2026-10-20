/**
 * Cliente HTTP central. Toda chamada ao backend passa por aqui — nenhum
 * componente deve usar `fetch` diretamente, para manter tratamento de erro,
 * timeout e headers consistentes em um único lugar.
 */

export class ApiError extends Error {
  status: number;
  issues?: unknown;

  constructor(message: string, status: number, issues?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.issues = issues;
  }
}

// No servidor (Server Components/Route Handlers) chamamos a API interna por caminho relativo;
// no cliente, o browser resolve "/api/..." contra a própria origem — nunca hardcode um host aqui.
const DEFAULT_TIMEOUT_MS = 10_000;

async function request<T>(
  path: string,
  options: RequestInit & { timeoutMs?: number } = {}
): Promise<T> {
  const { timeoutMs = DEFAULT_TIMEOUT_MS, ...init } = options;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(path, {
      ...init,
      signal: controller.signal,
      headers: {
        "Content-Type": "application/json",
        ...(init.headers ?? {}),
      },
      // Nunca envie credenciais para origens cruzadas por engano.
      credentials: "same-origin",
    });

    if (res.status === 204) {
      return undefined as T;
    }

    const payload = await res.json().catch(() => ({}));

    if (!res.ok) {
      throw new ApiError(
        // Rotas locais respondem { error }; os proxies do backend Java, { message }.
        payload?.error ?? payload?.message ?? `Erro na requisição (${res.status})`,
        res.status,
        payload?.issues
      );
    }

    return (payload?.data ?? payload) as T;
  } catch (err) {
    if (err instanceof ApiError) throw err;
    if (err instanceof DOMException && err.name === "AbortError") {
      throw new ApiError("Tempo de requisição esgotado. Tente novamente.", 408);
    }
    throw new ApiError("Falha de conexão com o servidor.", 0);
  } finally {
    clearTimeout(timeout);
  }
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
