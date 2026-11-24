import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { GerarMapaRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Desfaz a liberação da saída. */
export async function POST(req: Request) {
  return responder(async () => {
    const { eventoId } = await corpo(req, GerarMapaRequestSchema);
    await cozinhaBackend.desfazerLiberacao(eventoId);
    return { ok: true };
  });
}
