import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { SalvarOrcamentoRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Custos previstos que não saem das fichas (equipe sem escala, transporte, locação e outros). */
export async function PUT(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => {
    await eventoBackend.salvarOrcamento(idDe(id), await corpo(req, SalvarOrcamentoRequestSchema));
    return { ok: true };
  });
}
