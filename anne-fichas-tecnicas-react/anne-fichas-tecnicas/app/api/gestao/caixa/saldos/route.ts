import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { SaldosRequestSchema } from "@/lib/models/caixa";

export const dynamic = "force-dynamic";

/** Saldo de cada conta no fim do dia informado. */
export async function PUT(req: Request) {
  return responder(async () => {
    await caixaBackend.salvarSaldos(await corpo(req, SaldosRequestSchema));
    return { ok: true };
  });
}
