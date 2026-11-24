import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { CopiarCardapioRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => ({ copiados: await eventoBackend.copiarCardapio(idDe(id), (await corpo(req, CopiarCardapioRequestSchema)).origemEventoId) }));
}
