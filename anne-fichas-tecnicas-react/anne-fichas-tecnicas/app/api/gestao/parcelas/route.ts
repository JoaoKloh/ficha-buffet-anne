import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { UpdateParcelaRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";

export async function PUT(req: Request) {
  return responder(async () => {
    await eventoBackend.atualizarParcela(await corpo(req, UpdateParcelaRequestSchema));
    return { ok: true };
  });
}
export async function DELETE(req: Request) {
  return responder(() => eventoBackend.apagarParcela(idQuery(req)));
}
