import { NextRequest, NextResponse } from "next/server";
import { apagarCookiesDeSessao, authBackend, COOKIE_RENOVACAO, repassar } from "@/lib/server/authBackend";

export const dynamic = "force-dynamic";

// Espelha AutenticacaoController#logout (POST /auth/logout): revoga a sessão deste
// navegador no Java e apaga os cookies. Outros dispositivos continuam logados. Com o
// backend fora do ar, os cookies são apagados aqui mesmo (o refresh token segue válido
// no banco até vencer, mas o navegador já não o tem).
export async function POST(req: NextRequest) {
  try {
    const backendRes = await authBackend.logout(req.cookies.get(COOKIE_RENOVACAO)?.value);
    if (backendRes.ok) return repassar(backendRes);
  } catch (err) {
    console.error("[POST /api/auth/logout]", err instanceof Error ? err.message : "falha");
  }
  return apagarCookiesDeSessao(new NextResponse(null, { status: 204 }));
}
