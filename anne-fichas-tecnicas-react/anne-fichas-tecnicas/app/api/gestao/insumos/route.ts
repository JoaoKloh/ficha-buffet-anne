import { fichaBackend } from "@/lib/server/gestao/fichaBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreateInsumoRequestSchema, UpdateInsumoRequestSchema } from "@/lib/models/insumo";

export const dynamic = "force-dynamic";

/** Cadastro de itens (ingredientes, embalagens, louças...). ?situacao=ID diz se o item pode ser excluído. */
export async function GET(req: Request) {
  const sp = new URL(req.url).searchParams;
  if (sp.get("situacao")) return responder(() => fichaBackend.situacaoInsumo(idQuery(req, "situacao")));
  return responder(() => fichaBackend.listarInsumos(sp.get("busca") ?? ""), 200, "Erro ao carregar os itens.");
}
export async function POST(req: Request) {
  return responder(async () => fichaBackend.salvarInsumo(await corpo(req, CreateInsumoRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => fichaBackend.salvarInsumo(await corpo(req, UpdateInsumoRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => fichaBackend.apagarInsumo(idQuery(req)));
}
