import { equipeBackend } from "@/lib/server/gestao/equipeBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { AjustarEquipeRequestSchema } from "@/lib/models/equipe";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Edita o cadastro da pessoa da equipe: só os campos enviados mudam. */
export async function PATCH(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => equipeBackend.ajustar(idDe(id), await corpo(req, AjustarEquipeRequestSchema)));
}
