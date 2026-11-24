import { planoProducaoBackend } from "@/lib/server/gestao/planoProducaoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { CalcularCartaRequestSchema } from "@/lib/models/planoProducao";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Pratos da carta previstos no período viram as fichas a produzir (soma das fichas que cada prato leva). */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => planoProducaoBackend.calcularCarta(idDe(id), await corpo(req, CalcularCartaRequestSchema)));
}
