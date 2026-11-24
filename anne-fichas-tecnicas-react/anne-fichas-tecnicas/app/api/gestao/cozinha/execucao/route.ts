import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { SalvarExecucaoRequestSchema } from "@/lib/models/cozinha";

export const dynamic = "force-dynamic";

/** Grava o que a cozinha marcou (tarefa, compra, checklist, assinaturas). */
export async function POST(req: Request) {
  return responder(async () => cozinhaBackend.salvar(await corpo(req, SalvarExecucaoRequestSchema)));
}
