import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { PatchEventoRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Ficha do evento pronta para a tela: contrato, parcelas, extras, cardápio, resultado. */
export async function GET(_req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(() => eventoBackend.detalhe(idDe(id)));
}
/** Etapa, margem, cardápio fechado/reaberto e itens de envio. */
export async function PATCH(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => eventoBackend.patch(idDe(id), await corpo(req, PatchEventoRequestSchema)));
}
