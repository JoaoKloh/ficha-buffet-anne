import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { responder } from "@/lib/server/gestao/http";
import { UNIDADES_DRE, type UnidadeDre } from "@/lib/models/caixa";
import { hoje } from "@/lib/utils/datas";

export const dynamic = "force-dynamic";

/** DRE por unidade (ou comparando as unidades), por competência ou por caixa. */
export async function GET(req: Request) {
  const sp = new URL(req.url).searchParams;
  const ano = /^\d{4}$/.test(sp.get("ano") ?? "") ? sp.get("ano")! : hoje().slice(0, 4);
  const p = sp.get("periodo") ?? hoje().slice(5, 7);
  const periodo = p === "ano" || /^(0[1-9]|1[0-2])$/.test(p) ? p : hoje().slice(5, 7);
  const u = sp.get("unidade") ?? "";
  const unidade = (UNIDADES_DRE.some((x) => x[0] === u) ? u : "") as UnidadeDre;
  const regime = sp.get("regime") === "caixa" ? "caixa" : "competencia";
  return responder(() => caixaBackend.dre(ano, periodo, sp.get("previstos") !== "0", unidade, regime, sp.get("ratear") !== "0"), 200, "Erro ao montar a DRE.");
}
