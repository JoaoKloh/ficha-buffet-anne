import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { SalvarNotaFiscalRequestSchema } from "@/lib/models/estoque";

export const dynamic = "force-dynamic";

/** Lista de notas, ou uma nota com ?id=. */
export async function GET(req: Request) {
  if (new URL(req.url).searchParams.get("id")) return responder(() => estoqueBackend.obterNf(idQuery(req)));
  return responder(() => estoqueBackend.listarNfs());
}
/** Salvar rascunho novo. */
export async function POST(req: Request) {
  return responder(async () => estoqueBackend.salvarRascunho(null, await corpo(req, SalvarNotaFiscalRequestSchema)), 201);
}
/** Salvar rascunho existente (?id=). */
export async function PUT(req: Request) {
  return responder(async () => estoqueBackend.salvarRascunho(idQuery(req), await corpo(req, SalvarNotaFiscalRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => estoqueBackend.apagarNf(idQuery(req)));
}
