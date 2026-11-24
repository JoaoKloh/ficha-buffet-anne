import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { AjustarEtapaRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Marca/desmarca uma etapa manual ou dispensa/inclui a degustação. */
export async function PATCH(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => {
    await eventoBackend.ajustarEtapa(idDe(id), await corpo(req, AjustarEtapaRequestSchema));
    return { ok: true };
  });
}
