import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { LiberarMapaRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Libera as ordens para a equipe ou recolhe de volta. */
export async function PUT(req: Request) {
  return responder(async () => {
    await cozinhaBackend.liberarMapa(await corpo(req, LiberarMapaRequestSchema));
    return { ok: true };
  });
}
