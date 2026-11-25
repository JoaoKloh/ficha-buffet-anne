import { NextRequest, NextResponse } from "next/server";

/**
 * Protege todas as páginas: só /login fica pública. A sessão vive em dois cookies
 * httpOnly (setados por app/api/auth/login/route.ts): "accessToken" (JWT de 20 min,
 * o cookie some junto com o token) e "refreshToken" (7 dias, renova o access token).
 *
 * - Página sem nenhum dos dois: redireciona para /login.
 * - Página só com o refreshToken: segue; a primeira chamada à API recebe o 401
 *   abaixo e o apiClient (lib/api/client.ts) renova a sessão sozinho.
 * - /api/* (exceto /api/auth/*) sem accessToken mas com refreshToken: responde
 *   401 { code: "ACCESS_TOKEN_EXPIRED" } sem chamar o backend — é assim que o
 *   navegador fica sabendo que o access token venceu, já que não lê cookies httpOnly.
 *   Nada de redirect em /api: quebraria quem faz fetch nelas.
 *
 * Só checa a PRESENÇA dos cookies (não valida assinatura/expiração do JWT) —
 * cada chamada real ao backend já rejeita um token inválido/expirado com 401.
 * Nada disso usa localStorage: a sessão vive inteiramente nos cookies httpOnly.
 */
const PUBLIC_PATHS = ["/login"];
const COOKIE_ACESSO = "accessToken";
const COOKIE_RENOVACAO = "refreshToken";

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const temAcesso = req.cookies.has(COOKIE_ACESSO);
  const temRenovacao = req.cookies.has(COOKIE_RENOVACAO);

  if (pathname.startsWith("/api/")) {
    if (!pathname.startsWith("/api/auth/") && !temAcesso && temRenovacao) {
      return NextResponse.json(
        { message: "Sua sessão expirou.", code: "ACCESS_TOKEN_EXPIRED" },
        { status: 401 }
      );
    }
    return NextResponse.next();
  }

  if (PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`))) {
    return NextResponse.next();
  }

  if (!temAcesso && !temRenovacao) {
    const loginUrl = new URL("/login", req.url);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    // Todas as rotas (inclusive /api/*, tratadas acima sem redirect), exceto
    // internos do Next.js e favicon.
    "/((?!_next/static|_next/image|favicon.ico).*)",
  ],
};
