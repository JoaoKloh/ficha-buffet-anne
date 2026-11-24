import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { AssociarEquipeRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Escala em lote pelo checklist: a lista de ids é a escala inteira do evento. */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => eventoBackend.associarEquipe(idDe(id), await corpo(req, AssociarEquipeRequestSchema)));
}
