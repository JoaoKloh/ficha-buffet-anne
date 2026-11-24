import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CriarOcorrenciaRequestSchema, ResolverOcorrenciaRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Registra o que fugiu do planejado no evento. */
export async function POST(req: Request) {
  return responder(async () => cozinhaBackend.criarOcorrencia(await corpo(req, CriarOcorrenciaRequestSchema)), 201);
}
/** Marca a ocorrência como resolvida (ou reabre). */
export async function PATCH(req: Request) {
  return responder(async () => cozinhaBackend.resolverOcorrencia(idQuery(req), (await corpo(req, ResolverOcorrenciaRequestSchema)).resolvida));
}
