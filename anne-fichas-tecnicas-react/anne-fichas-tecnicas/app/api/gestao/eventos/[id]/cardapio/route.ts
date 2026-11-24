import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { AdicionarPratosCardapioRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Adiciona vários pratos ao cardápio de uma vez, com o fator e a praça de cada ficha. */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => ({ adicionados: await eventoBackend.adicionarPratos(idDe(id), await corpo(req, AdicionarPratosCardapioRequestSchema)) }), 201);
}
