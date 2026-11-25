// lib/server/authBackend.ts
import { NextResponse } from "next/server";

/**
 * Sessão no backend Java (AutenticacaoController, /auth/*). A sessão vive em dois cookies
 * httpOnly emitidos pelo Spring e repassados ao navegador como cookies da própria origem
 * do Next.js — nada de token em localStorage/JS:
 *   - accessToken: JWT de 20 min, enviado ao Java em toda chamada (ver lib/server/gestao/java.ts);
 *   - refreshToken: opaco, 7 dias, enviado ao Java SÓ aqui (login, refresh-token, logout).
 */

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export const COOKIE_ACESSO = "accessToken";
export const COOKIE_RENOVACAO = "refreshToken";

async function chamar(path: string, refreshToken: string | undefined, init: RequestInit = {}): Promise<Response> {
  return fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(refreshToken ? { Cookie: `${COOKIE_RENOVACAO}=${refreshToken}` } : {}),
      ...(init.headers ?? {}),
    },
    cache: "no-store",
  });
}

export const authBackend = {
  // forwardedFor repassa o X-Forwarded-For recebido pelo Next.js: sem ele o
  // backend veria sempre o IP deste servidor e o rate limit do login seria
  // compartilhado por todos os usuários. O refreshToken anterior (se houver)
  // vai junto para o Java encerrar a sessão antiga deste navegador.
  login(credentials: { username: string; password: string }, refreshAnterior?: string, forwardedFor?: string | null) {
    return chamar("/auth/login", refreshAnterior, {
      body: JSON.stringify(credentials),
      headers: forwardedFor ? { "X-Forwarded-For": forwardedFor } : undefined,
    });
  },

  renovar(refreshToken?: string) {
    return chamar("/auth/refresh-token", refreshToken);
  },

  logout(refreshToken?: string) {
    return chamar("/auth/logout", refreshToken);
  },
};

/**
 * Repassa ao navegador a resposta do Java: status, corpo JSON ({ message, code } nos erros)
 * e todos os Set-Cookie — inclusive os que apagam a sessão quando a renovação falha.
 */
export async function repassar(backendRes: Response): Promise<NextResponse> {
  const texto = await backendRes.text().catch(() => "");
  const res = texto.trim()
    ? new NextResponse(texto, { status: backendRes.status, headers: { "Content-Type": "application/json" } })
    : new NextResponse(null, { status: backendRes.ok ? 204 : backendRes.status });
  for (const cookie of backendRes.headers.getSetCookie()) {
    res.headers.append("set-cookie", cookie);
  }
  return res;
}

/** Apaga os cookies de sessão sem depender do Java (logout com o backend fora do ar). */
export function apagarCookiesDeSessao(res: NextResponse): NextResponse {
  for (const nome of [COOKIE_ACESSO, COOKIE_RENOVACAO]) {
    res.cookies.set(nome, "", { httpOnly: true, sameSite: "strict", path: "/", maxAge: 0 });
  }
  return res;
}
