import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { responder } from "@/lib/server/gestao/http";
import { FILTROS_CONTAS, type FiltroContas } from "@/lib/models/caixa";
import { unidadeDaQuery } from "../unidade";

export const dynamic = "force-dynamic";

/** Contas a pagar ou a receber, pelo vencimento. */
export async function GET(req: Request) {
  const sp = new URL(req.url).searchParams;
  const tipo = sp.get("tipo") === "receber" ? "receber" : "pagar";
  const f = sp.get("filtro") as FiltroContas | null;
  return responder(() => caixaBackend.contas(tipo, f && FILTROS_CONTAS.some((x) => x[0] === f) ? f : "abertas", unidadeDaQuery(req)), 200, "Erro ao carregar as contas.");
}
