import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { SalvarLogisticaRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Plano de transporte: veículos, motorista, horários de saída, montagem e retorno. */
export async function PUT(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => {
    await eventoBackend.salvarLogistica(idDe(id), await corpo(req, SalvarLogisticaRequestSchema));
    return { ok: true };
  });
}
