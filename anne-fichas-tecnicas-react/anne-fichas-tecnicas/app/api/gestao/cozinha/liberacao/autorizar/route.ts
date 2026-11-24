import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { AutorizarLiberacaoRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Libera a saída (com pendências justificadas quando nem tudo está pronto) e conclui a ordem de expedição. */
export async function POST(req: Request) {
  return responder(async () => {
    await cozinhaBackend.autorizarLiberacao(await corpo(req, AutorizarLiberacaoRequestSchema));
    return { ok: true };
  });
}
