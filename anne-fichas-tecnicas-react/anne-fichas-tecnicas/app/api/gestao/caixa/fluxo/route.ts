import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { responder } from "@/lib/server/gestao/http";
import { unidadeDaQuery } from "../unidade";

export const dynamic = "force-dynamic";

/** Saldos das contas e fluxo projetado pelos vencimentos. */
export async function GET(req: Request) {
  return responder(() => caixaBackend.fluxo(unidadeDaQuery(req)), 200, "Erro ao montar o fluxo de caixa.");
}
