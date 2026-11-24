import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreateCategoriaRequestSchema, UpdateCategoriaRequestSchema } from "@/lib/models/caixa";

export const dynamic = "force-dynamic";

export async function POST(req: Request) {
  return responder(async () => caixaBackend.criarCategoria(await corpo(req, CreateCategoriaRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => caixaBackend.atualizarCategoria(await corpo(req, UpdateCategoriaRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => caixaBackend.apagarCategoria(idQuery(req)));
}
