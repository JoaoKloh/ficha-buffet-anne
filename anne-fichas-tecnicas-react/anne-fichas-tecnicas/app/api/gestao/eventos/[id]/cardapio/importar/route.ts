import { z } from "zod";
import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { ImportarCardapioRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Confere o texto colado contra as fichas, sem gravar. */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => eventoBackend.analisarImportacao(idDe(id), (await corpo(req, z.object({ texto: z.string().min(1, "Cole o cardápio primeiro.") }))).texto));
}
/** Grava as linhas conferidas. */
export async function PUT(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => ({ adicionados: await eventoBackend.confirmarImportacao(idDe(id), await corpo(req, ImportarCardapioRequestSchema)) }));
}
