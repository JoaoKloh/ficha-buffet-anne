import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { MovimentarLoteRequestSchema } from "@/lib/models/estoque";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Uso ou descarte de parte do lote. */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => estoqueBackend.movimentarLote(idDe(id), await corpo(req, MovimentarLoteRequestSchema)));
}
