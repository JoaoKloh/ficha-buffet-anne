import { NextRequest, NextResponse } from "next/server";
import { authBackend, COOKIE_RENOVACAO, repassar } from "@/lib/server/authBackend";

export const dynamic = "force-dynamic";

// Espelha AutenticacaoController#login (POST /auth/login). Passa pelo servidor
// Next.js (em vez do browser chamar o backend direto) para que os cookies
// httpOnly "accessToken" e "refreshToken" devolvidos pelo Spring sejam
// repassados como cookies da PRÓPRIA origem do frontend — nada de token em
// localStorage/JS. Erro (ex.: 401 INVALID_CREDENTIALS) segue com { message, code }.
export async function POST(req: NextRequest) {
  const body = await req.json().catch(() => null);
  if (!body || typeof body.username !== "string" || typeof body.password !== "string") {
    return NextResponse.json({ error: "Usuário e senha são obrigatórios." }, { status: 400 });
  }

  try {
    const backendRes = await authBackend.login(
      { username: body.username, password: body.password },
      req.cookies.get(COOKIE_RENOVACAO)?.value,
      req.headers.get("x-forwarded-for")
    );
    return repassar(backendRes);
  } catch (err) {
    console.error("[POST /api/auth/login]", err);
    return NextResponse.json({ error: "Erro ao autenticar no backend." }, { status: 502 });
  }
}
