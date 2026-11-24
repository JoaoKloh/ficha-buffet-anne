import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreateMovimentoRequestSchema } from "@/lib/models/estoque";

export const dynamic = "force-dynamic";

export async function GET(req: Request) {
  return responder(() => estoqueBackend.movimentos(new URL(req.url).searchParams.get("filtro") ?? "todos"));
}
export async function POST(req: Request) {
  return responder(async () => estoqueBackend.criarMovimento(await corpo(req, CreateMovimentoRequestSchema)), 201);
}
export async function DELETE(req: Request) {
  return responder(() => estoqueBackend.apagarMovimento(idQuery(req)));
}
