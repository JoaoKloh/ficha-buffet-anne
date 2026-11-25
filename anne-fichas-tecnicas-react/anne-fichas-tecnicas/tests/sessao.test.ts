import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { NextRequest } from "next/server";

/**
 * Renovação automática da sessão no apiClient (lib/api/client.ts) e o 401 do
 * middleware.ts. fetch é simulado; cada teste carrega o módulo do zero (estado limpo).
 */

type Rota = (init: RequestInit | undefined, chamada: number) => Response | Promise<Response>;

const json = (status: number, corpo: unknown) =>
  new Response(JSON.stringify(corpo), { status, headers: { "Content-Type": "application/json" } });
const expirado = () => json(401, { message: "Sua sessão expirou.", code: "ACCESS_TOKEN_EXPIRED" });
const ok = (data: unknown) => json(200, { data });
const vazio = (status = 204) => new Response(null, { status });

let rotas: Record<string, Rota>;
let chamadas: string[];
let replace: ReturnType<typeof vi.fn>;

function contar(path: string) {
  return chamadas.filter((c) => c === path).length;
}

async function carregar() {
  vi.resetModules();
  return import("@/lib/api/client");
}

beforeEach(() => {
  rotas = {};
  chamadas = [];
  replace = vi.fn();
  vi.stubGlobal("window", { location: { pathname: "/eventos", replace } });
  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string, init?: RequestInit) => {
      chamadas.push(path);
      const rota = rotas[path];
      if (!rota) throw new Error(`rota não simulada: ${path}`);
      return rota(init, contar(path));
    })
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("apiClient — renovação da sessão", () => {
  it("requisição normal não chama a renovação", async () => {
    rotas["/api/x"] = () => ok({ a: 1 });
    const { apiClient } = await carregar();
    await expect(apiClient.get("/api/x")).resolves.toEqual({ a: 1 });
    expect(chamadas).toEqual(["/api/x"]);
  });

  it("401 ACCESS_TOKEN_EXPIRED renova e repete a requisição original uma vez", async () => {
    rotas["/api/x"] = (_i, n) => (n === 1 ? expirado() : ok("depois"));
    rotas["/api/auth/refresh-token"] = () => vazio();
    const { apiClient } = await carregar();

    await expect(apiClient.get("/api/x")).resolves.toBe("depois");
    expect(chamadas).toEqual(["/api/x", "/api/auth/refresh-token", "/api/x"]);
    expect(replace).not.toHaveBeenCalled();
  });

  it("repete o POST com o mesmo corpo", async () => {
    const corpos: unknown[] = [];
    rotas["/api/x"] = (init, n) => {
      corpos.push(init?.body);
      return n === 1 ? expirado() : ok(true);
    };
    rotas["/api/auth/refresh-token"] = () => vazio();
    const { apiClient } = await carregar();

    await apiClient.post("/api/x", { nome: "Evento" });
    expect(corpos).toEqual(['{"nome":"Evento"}', '{"nome":"Evento"}']);
  });

  it("403 não tenta renovar nem encerra a sessão", async () => {
    rotas["/api/x"] = () => json(403, { message: "Sem permissão.", code: "ACCESS_DENIED" });
    const { apiClient, ApiError } = await carregar();

    const erro = (await apiClient.get("/api/x").catch((e: unknown) => e)) as InstanceType<typeof ApiError>;
    expect(erro).toBeInstanceOf(ApiError);
    expect(erro.status).toBe(403);
    expect(erro.code).toBe("ACCESS_DENIED");
    expect(chamadas).toEqual(["/api/x"]);
    expect(replace).not.toHaveBeenCalled();
  });

  it("401 não renovável não chama refresh: encerra a sessão e vai para /login", async () => {
    rotas["/api/x"] = () => json(401, { message: "Sessão inválida.", code: "INVALID_TOKEN" });
    rotas["/api/auth/logout"] = () => vazio();
    const { apiClient } = await carregar();

    await expect(apiClient.get("/api/x")).rejects.toMatchObject({ status: 401, code: "INVALID_TOKEN" });
    expect(contar("/api/auth/refresh-token")).toBe(0);
    expect(contar("/api/auth/logout")).toBe(1);
    expect(replace).toHaveBeenCalledWith("/login");
  });

  it("renovação recusada: faz logout, vai para /login e não repete a requisição", async () => {
    rotas["/api/x"] = () => expirado();
    rotas["/api/auth/refresh-token"] = () => json(401, { code: "REFRESH_TOKEN_INVALID" });
    rotas["/api/auth/logout"] = () => vazio();
    const { apiClient } = await carregar();

    await expect(apiClient.get("/api/x")).rejects.toMatchObject({ status: 401 });
    expect(chamadas).toEqual(["/api/x", "/api/auth/refresh-token", "/api/auth/logout"]);
    expect(replace).toHaveBeenCalledTimes(1);
    expect(replace).toHaveBeenCalledWith("/login");
  });

  it("renovação indisponível (502) não derruba a sessão", async () => {
    rotas["/api/x"] = () => expirado();
    rotas["/api/auth/refresh-token"] = () => json(502, { message: "fora do ar" });
    const { apiClient } = await carregar();

    await expect(apiClient.get("/api/x")).rejects.toMatchObject({ status: 401 });
    expect(contar("/api/auth/logout")).toBe(0);
    expect(replace).not.toHaveBeenCalled();
  });

  it("sem laço: 401 expirado de novo depois de renovar não renova outra vez", async () => {
    rotas["/api/x"] = () => expirado();
    rotas["/api/auth/refresh-token"] = () => vazio();
    rotas["/api/auth/logout"] = () => vazio();
    const { apiClient } = await carregar();

    await expect(apiClient.get("/api/x")).rejects.toMatchObject({ status: 401 });
    expect(contar("/api/x")).toBe(2);
    expect(contar("/api/auth/refresh-token")).toBe(1);
    expect(replace).toHaveBeenCalledWith("/login");
  });

  it("rotas /api/auth/* nunca disparam renovação (login errado, refresh recursivo)", async () => {
    rotas["/api/auth/login"] = () => json(401, { message: "Usuário ou senha inválidos.", code: "ACCESS_TOKEN_EXPIRED" });
    const { apiClient } = await carregar();

    await expect(apiClient.post("/api/auth/login", {})).rejects.toMatchObject({ status: 401 });
    expect(chamadas).toEqual(["/api/auth/login"]);
    expect(replace).not.toHaveBeenCalled();
  });

  it("requisições simultâneas com 401 fazem um único refresh e cada uma é repetida uma vez", async () => {
    const tentativas: Record<string, number> = {};
    for (const p of ["/api/a", "/api/b", "/api/c"]) {
      rotas[p] = () => {
        tentativas[p] = (tentativas[p] ?? 0) + 1;
        return tentativas[p] === 1 ? expirado() : ok(p);
      };
    }
    let liberar!: () => void;
    const segurar = new Promise<void>((r) => (liberar = r));
    rotas["/api/auth/refresh-token"] = async () => {
      await segurar; // mantém a renovação em andamento enquanto as outras chegam
      return vazio();
    };
    const { apiClient } = await carregar();

    const todas = Promise.all([apiClient.get("/api/a"), apiClient.get("/api/b"), apiClient.get("/api/c")]);
    await vi.waitFor(() => expect(contar("/api/auth/refresh-token")).toBe(1));
    liberar();

    await expect(todas).resolves.toEqual(["/api/a", "/api/b", "/api/c"]);
    expect(contar("/api/auth/refresh-token")).toBe(1);
    expect(tentativas).toEqual({ "/api/a": 2, "/api/b": 2, "/api/c": 2 });
  });

  it("401 que chega depois de uma renovação concluída só repete, sem novo refresh", async () => {
    let liberarB!: () => void;
    const segurarB = new Promise<void>((r) => (liberarB = r));
    rotas["/api/a"] = (_i, n) => (n === 1 ? expirado() : ok("a"));
    rotas["/api/b"] = async (_i, n) => {
      if (n === 1) {
        await segurarB; // enviada antes da renovação, responde depois dela
        return expirado();
      }
      return ok("b");
    };
    rotas["/api/auth/refresh-token"] = () => vazio();
    const { apiClient } = await carregar();

    const b = apiClient.get("/api/b");
    await expect(apiClient.get("/api/a")).resolves.toBe("a");
    liberarB();
    await expect(b).resolves.toBe("b");
    expect(contar("/api/auth/refresh-token")).toBe(1);
  });

  it("vários 401 não renováveis simultâneos fazem um único logout/redirect", async () => {
    rotas["/api/a"] = () => json(401, { code: "AUTHENTICATION_REQUIRED" });
    rotas["/api/b"] = () => json(401, { code: "AUTHENTICATION_REQUIRED" });
    let liberar!: () => void;
    const segurar = new Promise<void>((r) => (liberar = r));
    rotas["/api/auth/logout"] = async () => {
      await segurar;
      return vazio();
    };
    const { apiClient } = await carregar();

    const todas = Promise.allSettled([apiClient.get("/api/a"), apiClient.get("/api/b")]);
    await vi.waitFor(() => expect(contar("/api/b")).toBe(1));
    liberar();
    await todas;
    expect(contar("/api/auth/logout")).toBe(1);
    expect(replace).toHaveBeenCalledTimes(1);
  });

  it("já no /login não redireciona de novo (sem laço de redirect)", async () => {
    vi.stubGlobal("window", { location: { pathname: "/login", replace } });
    rotas["/api/x"] = () => json(401, { code: "AUTHENTICATION_REQUIRED" });
    rotas["/api/auth/logout"] = () => vazio();
    const { apiClient } = await carregar();

    await apiClient.get("/api/x").catch(() => undefined);
    expect(replace).not.toHaveBeenCalled();
  });

  it("usa a trava entre abas (Web Locks) quando o navegador a oferece", async () => {
    const request = vi.fn((_nome: string, fn: () => Promise<unknown>) => fn());
    vi.stubGlobal("navigator", { locks: { request } });
    rotas["/api/x"] = (_i, n) => (n === 1 ? expirado() : ok(1));
    rotas["/api/auth/refresh-token"] = () => vazio();
    const { apiClient } = await carregar();

    await apiClient.get("/api/x");
    expect(request).toHaveBeenCalledTimes(1);
    expect(request.mock.calls[0]?.[0]).toBe("anne-renovacao-sessao");
  });

  it("logout do authApi revoga a sessão e vai para /login", async () => {
    rotas["/api/auth/logout"] = () => vazio();
    vi.resetModules();
    const { authApi } = await import("@/lib/api/auth");

    await authApi.logout();
    expect(chamadas).toEqual(["/api/auth/logout"]);
    expect(replace).toHaveBeenCalledWith("/login");
  });
});

describe("middleware — sessão por cookies", () => {
  async function rodar(path: string, cookies: string[]) {
    const { middleware } = await import("@/middleware");
    const req = new NextRequest(`http://localhost${path}`, {
      headers: cookies.length ? { cookie: cookies.map((c) => `${c}=v`).join("; ") } : {},
    });
    return middleware(req);
  }

  it("API sem accessToken mas com refreshToken responde 401 ACCESS_TOKEN_EXPIRED", async () => {
    const res = await rodar("/api/gestao/eventos", ["refreshToken"]);
    expect(res.status).toBe(401);
    expect(await res.json()).toMatchObject({ code: "ACCESS_TOKEN_EXPIRED" });
  });

  it("API com accessToken, sem cookie algum ou em /api/auth/* segue para a rota", async () => {
    for (const [path, cookies] of [
      ["/api/gestao/eventos", ["accessToken", "refreshToken"]],
      ["/api/gestao/eventos", []],
      ["/api/auth/refresh-token", ["refreshToken"]],
    ] as const) {
      const res = await rodar(path, [...cookies]);
      expect(res.headers.get("x-middleware-next")).toBe("1");
    }
  });

  it("página só com refreshToken abre (a renovação acontece na primeira chamada)", async () => {
    const res = await rodar("/eventos", ["refreshToken"]);
    expect(res.headers.get("x-middleware-next")).toBe("1");
  });

  it("página sem cookie de sessão redireciona para /login", async () => {
    const res = await rodar("/eventos", []);
    expect(res.status).toBe(307);
    expect(res.headers.get("location")).toBe("http://localhost/login");
  });
});
