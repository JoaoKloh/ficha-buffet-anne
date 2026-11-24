import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { idDe, responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Marca como pago ou recebido hoje. */
export async function POST(_req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => {
    await caixaBackend.marcarPago(idDe(id));
    return { ok: true };
  });
}
