import { planoProducaoBackend } from "@/lib/server/gestao/planoProducaoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { ItemPlanoAcaoRequestSchema } from "@/lib/models/planoProducao";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Volta o item para a produção e apaga o lote (422 se o lote já teve uso ou descarte). */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => {
    await planoProducaoBackend.desfazerItem(idDe(id), (await corpo(req, ItemPlanoAcaoRequestSchema)).k);
    return { ok: true };
  });
}
