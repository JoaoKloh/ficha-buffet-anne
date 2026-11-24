import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { idDe, responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Diárias da escala ainda fora do caixa viram contas a pagar no dia do evento. */
export async function POST(_req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(() => eventoBackend.lancarEquipe(idDe(id)), 201);
}
