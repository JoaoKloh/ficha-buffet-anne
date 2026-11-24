import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { AcaoOrdemRequestSchema, PlanoOrdemRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Ação da equipe numa ordem: iniciar, concluir, conferir (gera lote e baixa), bloquear, desbloquear, reabrir, responsável. */
export async function POST(req: Request) {
  return responder(async () => {
    await cozinhaBackend.agirOrdem(await corpo(req, AcaoOrdemRequestSchema));
    return { ok: true };
  });
}
/** Gestão: prazo, horário limite, setor e responsável da ordem. */
export async function PUT(req: Request) {
  return responder(async () => {
    await cozinhaBackend.planejarOrdem(await corpo(req, PlanoOrdemRequestSchema));
    return { ok: true };
  });
}
