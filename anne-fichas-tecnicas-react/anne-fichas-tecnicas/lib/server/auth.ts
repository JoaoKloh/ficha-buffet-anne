import { NextRequest } from "next/server";

/**
 * Guard de autenticação/autorização para rotas de API que alteram dados.
 *
 * Este é um placeholder de referência: valida uma API key simples via header,
 * comparada de forma segura contra timing attacks. Em produção, troque por
 * validação de sessão (ex: cookies httpOnly + biblioteca de sessão) ou JWT
 * assinado, e NUNCA aceite a chave via query string (fica em logs de acesso).
 *
 * A chave nunca deve ser exposta ao cliente — por isso vive só em
 * process.env.API_SECRET_KEY (sem prefixo NEXT_PUBLIC_).
 */
export class UnauthorizedError extends Error {
  constructor(message = "Não autorizado") {
    super(message);
    this.name = "UnauthorizedError";
  }
}

function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let mismatch = 0;
  for (let i = 0; i < a.length; i++) {
    mismatch |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return mismatch === 0;
}

export function requireAuth(req: NextRequest): void {
  const expected = process.env.API_SECRET_KEY;

  // Backend de referência sem chave configurada (ambiente local/demo): não bloqueia,
  // mas isso NUNCA deve acontecer em produção — configure API_SECRET_KEY no deploy real.
  if (!expected) return;

  const provided = req.headers.get("x-api-key") ?? "";
  if (!provided || !timingSafeEqual(provided, expected)) {
    throw new UnauthorizedError();
  }
}
