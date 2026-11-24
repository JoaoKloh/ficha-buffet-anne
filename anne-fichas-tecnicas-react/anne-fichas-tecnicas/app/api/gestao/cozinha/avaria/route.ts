import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { AvariaRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Lança no estoque a perda das louças que faltaram no retorno. */
export async function POST(req: Request) {
  return responder(async () => ({ lancado: await cozinhaBackend.avaria(await corpo(req, AvariaRequestSchema)) }), 201);
}
