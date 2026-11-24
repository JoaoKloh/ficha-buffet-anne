import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { idDe, responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Fecha o evento: status Acertado e retrato do resultado com data e quem fechou. */
export async function POST(_req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(() => eventoBackend.fechar(idDe(id)));
}
