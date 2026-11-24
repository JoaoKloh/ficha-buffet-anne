import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreateLoteRequestSchema, UpdateLoteRequestSchema } from "@/lib/models/estoque";

export const dynamic = "force-dynamic";

export async function GET(req: Request) {
  return responder(() => estoqueBackend.lote(idQuery(req)));
}
export async function POST(req: Request) {
  return responder(async () => estoqueBackend.criarLote(await corpo(req, CreateLoteRequestSchema)), 201);
}
/** Lote sem uso: corpo completo. Com uso: só validade, responsável e observações. */
export async function PUT(req: Request) {
  return responder(async () => estoqueBackend.atualizarLote(idQuery(req), await corpo(req, CreateLoteRequestSchema.or(UpdateLoteRequestSchema.omit({ id: true })))));
}
export async function DELETE(req: Request) {
  return responder(() => estoqueBackend.apagarLote(idQuery(req)));
}
