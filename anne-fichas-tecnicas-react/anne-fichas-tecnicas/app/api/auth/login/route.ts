import { NextRequest, NextResponse } from "next/server";
import { pratoBackend, PratoBackendError } from "@/lib/server/pratoBackend";

export const dynamic = "force-dynamic";

// Espelha AutenticacaoController#login (POST /auth/login). Passa pelo servidor
// Next.js (em vez do browser chamar o backend direto) para que o cookie
// httpOnly "accessToken" devolvido pelo Spring seja repassado como um cookie
// da PRÓPRIA origem do frontend — nada de token em localStorage/JS.
export async function POST(req: NextRequest) {
  const body = await req.json().catch(() => null);
  if (!body || typeof body.username !== "string" || typeof body.password !== "string") {
    return NextResponse.json({ error: "Usuário e senha são obrigatórios." }, { status: 400 });
  }

  try {
    const backendRes = await pratoBackend.login({ username: body.username, password: body.password });

    const res = new NextResponse(null, { status: 204 });
    for (const cookie of backendRes.headers.getSetCookie()) {
      res.headers.append("set-cookie", cookie);
    }
    return res;
  } catch (err) {
    if (err instanceof PratoBackendError) {
      return NextResponse.json({ error: err.message }, { status: err.status });
    }
    console.error("[POST /api/auth/login]", err);
    return NextResponse.json({ error: "Erro ao autenticar no backend." }, { status: 502 });
  }
}
