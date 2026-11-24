import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { PatchItemCardapioRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Situação (Sim, A definir, Não) e fator por convidado, alterados direto na lista. */
export async function PATCH(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => eventoBackend.patchItemCardapio(idDe(id), await corpo(req, PatchItemCardapioRequestSchema)));
}
