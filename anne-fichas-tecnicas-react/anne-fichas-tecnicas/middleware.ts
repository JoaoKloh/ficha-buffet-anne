import { NextRequest, NextResponse } from "next/server";

/**
 * Protege todas as páginas: só /login fica pública. Se o cookie de sessão
 * (accessToken, httpOnly — setado por app/api/auth/login/route.ts) não
 * estiver presente, redireciona para /login em vez de renderizar a página.
 *
 * Só checa a PRESENÇA do cookie (não valida assinatura/expiração do JWT) —
 * cada chamada real ao backend (ver lib/server/pratoBackend.ts) já rejeita
 * um token inválido/expirado com 401. Isso evita trazer uma lib de JWT para
 * o middleware só para reforçar uma checagem que o backend já faz.
 *
 * Nada disso usa localStorage: a sessão vive inteiramente no cookie httpOnly,
 * inacessível a JS no browser.
 */
const PUBLIC_PATHS = ["/login"];

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;

  if (PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`))) {
    return NextResponse.next();
  }

  const hasSession = req.cookies.has("accessToken");
  if (!hasSession) {
    const loginUrl = new URL("/login", req.url);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    // Todas as rotas, exceto: internos do Next.js, favicon e /api/* — rotas de
    // API devolvem JSON, então um redirect ali quebraria quem faz fetch nelas;
    // cada uma já valida a sessão sozinha (ex.: repassando o 401 do backend).
    "/((?!_next/static|_next/image|favicon.ico|api/).*)",
  ],
};
