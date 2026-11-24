import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { SalvarSetoresRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Setores da cozinha (renomear, desativar, criar). */
export async function PUT(req: Request) {
  return responder(async () => {
    await cozinhaBackend.salvarSetores(await corpo(req, SalvarSetoresRequestSchema));
    return { ok: true };
  });
}
