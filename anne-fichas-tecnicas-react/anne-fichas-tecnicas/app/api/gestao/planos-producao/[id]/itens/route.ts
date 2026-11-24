import { planoProducaoBackend } from "@/lib/server/gestao/planoProducaoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { BackendError } from "@/lib/server/gestao/java";
import { AdicionarItensPlanoRequestSchema, AjustarItemPlanoRequestSchema } from "@/lib/models/planoProducao";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

function itemDe(req: Request): string {
  const k = new URL(req.url).searchParams.get("k")?.trim();
  if (!k) throw new BackendError("Item do plano obrigatório.", 400);
  return k;
}

/** Adiciona fichas ao plano, com a quantidade na unidade da ficha. */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => planoProducaoBackend.adicionarItens(idDe(id), await corpo(req, AdicionarItensPlanoRequestSchema)), 201);
}
/** Ajusta a quantidade de um item ainda não concluído. */
export async function PATCH(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => planoProducaoBackend.ajustarItem(idDe(id), itemDe(req), (await corpo(req, AjustarItemPlanoRequestSchema)).qtd));
}
/** Tira um item ainda não concluído do plano. */
export async function DELETE(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(() => planoProducaoBackend.removerItem(idDe(id), itemDe(req)));
}
