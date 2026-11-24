import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { idDe, responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Desfaz o último pagamento ou recebimento. */
export async function POST(_req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(() => caixaBackend.desfazerPagamento(idDe(id)));
}
