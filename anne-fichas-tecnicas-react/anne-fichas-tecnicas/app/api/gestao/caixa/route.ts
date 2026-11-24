import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { responder } from "@/lib/server/gestao/http";
import { FILTROS_CAIXA, type FiltroCaixa } from "@/lib/models/caixa";
import { hoje } from "@/lib/utils/datas";
import { unidadeDaQuery } from "./unidade";

export const dynamic = "force-dynamic";

/** Aba Lançamentos do Caixa: KPIs do mês, lista filtrada e categorias, na unidade pedida. */
export async function GET(req: Request) {
  const sp = new URL(req.url).searchParams;
  const mes = /^\d{4}-\d{2}$/.test(sp.get("mes") ?? "") ? sp.get("mes")! : hoje().slice(0, 7);
  const f = sp.get("filtro") as FiltroCaixa | null;
  return responder(() => caixaBackend.tela(mes, f && FILTROS_CAIXA.some((x) => x[0] === f) ? f : "todos", unidadeDaQuery(req)), 200, "Erro ao carregar o caixa.");
}
