import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { AtualizarContratoRequestSchema, RegistrarContratoRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => {
    await eventoBackend.registrarContrato(idDe(id), await corpo(req, RegistrarContratoRequestSchema));
    return { ok: true };
  }, 201);
}
export async function PUT(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => {
    await eventoBackend.atualizarContrato(idDe(id), await corpo(req, AtualizarContratoRequestSchema));
    return { ok: true };
  });
}
export async function DELETE(_req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(() => eventoBackend.apagarContrato(idDe(id)));
}
