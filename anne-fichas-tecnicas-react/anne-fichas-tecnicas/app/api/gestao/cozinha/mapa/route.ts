import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { GerarMapaRequestSchema, MestreMapaRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Gera o mapa de produção do evento (só a gestão vê até liberar). */
export async function POST(req: Request) {
  return responder(async () => {
    const { eventoId } = await corpo(req, GerarMapaRequestSchema);
    await cozinhaBackend.gerarMapa(eventoId);
    return { ok: true };
  }, 201);
}
/** Ficha mestre: responsável geral e horários de serviço. */
export async function PUT(req: Request) {
  return responder(async () => {
    await cozinhaBackend.salvarMestre(await corpo(req, MestreMapaRequestSchema));
    return { ok: true };
  });
}
