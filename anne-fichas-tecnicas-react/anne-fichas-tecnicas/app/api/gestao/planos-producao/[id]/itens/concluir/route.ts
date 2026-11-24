import { planoProducaoBackend } from "@/lib/server/gestao/planoProducaoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { ItemPlanoAcaoRequestSchema } from "@/lib/models/planoProducao";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Conclui o item: lote em Pré-preparos com a validade da ficha e baixa dos insumos. */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => planoProducaoBackend.concluirItem(idDe(id), (await corpo(req, ItemPlanoAcaoRequestSchema)).k), 201);
}
