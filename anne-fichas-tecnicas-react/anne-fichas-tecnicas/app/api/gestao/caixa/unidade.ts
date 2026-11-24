import { FILTROS_UNIDADE, type FiltroUnidade } from "@/lib/models/caixa";

/** Filtro de unidade da query (?unidade=); valor desconhecido vira "todas". */
export function unidadeDaQuery(req: Request): FiltroUnidade {
  const u = new URL(req.url).searchParams.get("unidade") ?? "";
  return FILTROS_UNIDADE.some((x) => x[0] === u) ? (u as FiltroUnidade) : "";
}
