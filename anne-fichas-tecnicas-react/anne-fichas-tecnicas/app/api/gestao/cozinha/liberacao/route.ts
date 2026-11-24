import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { SalvarLiberacaoRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Conferências e horário de saída da liberação para expedição. */
export async function PUT(req: Request) {
  return responder(async () => {
    await cozinhaBackend.salvarLiberacao(await corpo(req, SalvarLiberacaoRequestSchema));
    return { ok: true };
  });
}
