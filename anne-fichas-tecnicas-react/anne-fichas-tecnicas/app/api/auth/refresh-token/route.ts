import { NextRequest, NextResponse } from "next/server";
import { authBackend, COOKIE_RENOVACAO, repassar } from "@/lib/server/authBackend";

export const dynamic = "force-dynamic";

// Espelha AutenticacaoController#refreshToken (POST /auth/refresh-token). Chamada só pelo
// apiClient (lib/api/client.ts) quando uma requisição volta 401 ACCESS_TOKEN_EXPIRED:
// 204 com os cookies novos, ou 401 REFRESH_TOKEN_INVALID com os cookies apagados.
export async function POST(req: NextRequest) {
  try {
    return await repassar(await authBackend.renovar(req.cookies.get(COOKIE_RENOVACAO)?.value));
  } catch (err) {
    console.error("[POST /api/auth/refresh-token]", err instanceof Error ? err.message : "falha");
    // Backend fora do ar: a sessão não é dada como encerrada (não apaga cookies).
    return NextResponse.json({ message: "Não foi possível falar com o servidor. Tente de novo em instantes." }, { status: 502 });
  }
}
