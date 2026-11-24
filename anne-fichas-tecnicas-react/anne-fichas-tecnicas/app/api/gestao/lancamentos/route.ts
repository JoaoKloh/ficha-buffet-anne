import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreateLancamentoRequestSchema, UpdateLancamentoRequestSchema } from "@/lib/models/caixa";

export const dynamic = "force-dynamic";

export async function GET(req: Request) {
  return responder(() => caixaBackend.obterLancamento(idQuery(req)));
}
export async function POST(req: Request) {
  return responder(async () => caixaBackend.criarLancamento(await corpo(req, CreateLancamentoRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => caixaBackend.atualizarLancamento(await corpo(req, UpdateLancamentoRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => caixaBackend.apagarLancamento(idQuery(req)));
}
