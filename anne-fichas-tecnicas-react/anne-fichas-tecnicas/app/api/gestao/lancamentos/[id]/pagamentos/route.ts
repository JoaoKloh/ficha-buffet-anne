import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { PagamentoRequestSchema } from "@/lib/models/caixa";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Registra um pagamento ou recebimento, inteiro ou de uma parte. */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => caixaBackend.registrarPagamento(idDe(id), await corpo(req, PagamentoRequestSchema)));
}
