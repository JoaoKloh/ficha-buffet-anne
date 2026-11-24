import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { RateioRequestSchema } from "@/lib/models/caixa";

export const dynamic = "force-dynamic";

/** Percentuais do rateio da estrutura compartilhada. */
export async function PUT(req: Request) {
  return responder(async () => {
    await caixaBackend.salvarRateio(await corpo(req, RateioRequestSchema));
    return { ok: true };
  });
}
