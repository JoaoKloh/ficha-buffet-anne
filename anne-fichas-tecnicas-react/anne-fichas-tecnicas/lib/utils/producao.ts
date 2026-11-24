import type { CozinhaProjecaoDTO, ExecucaoResponseDTO, OrdemProducaoDTO, PratoProjecaoDTO } from "@/lib/models/cozinha";
import { STATUS_ORDEM, type StatusOrdem } from "@/lib/models/dominio";
import { addDays, hoje } from "./datas";

/** Situação de um prato na lista de produção (protótipo: prodStatus). */
export function statusProducao(pj: CozinhaProjecaoDTO, x: PratoProjecaoDTO, ex: Partial<ExecucaoResponseDTO>): [string, "Concluída" | "Em produção" | "Atrasada" | "Pendente"] {
  const prazo = pj.data && x.dnf != null ? addDays(pj.data, -x.dnf) : "";
  const t = hoje();
  if ((Number(ex.produzido) || 0) > 0 || ex.fim) return ["good", "Concluída"];
  if (ex.inicio) return [prazo && prazo < t ? "bad" : "accent", "Em produção"];
  if (prazo && prazo < t && pj.data >= t) return ["bad", "Atrasada"];
  return ["", "Pendente"];
}

export const statusOrdemLabel = (s: StatusOrdem): string => STATUS_ORDEM.find((x) => x[0] === s)?.[1] ?? "Pendente";
export const statusOrdemTom = (s: StatusOrdem): string => STATUS_ORDEM.find((x) => x[0] === s)?.[2] ?? "";

/** Pill da ordem: atrasada vence o status. */
export const situacaoOrdem = (o: Pick<OrdemProducaoDTO, "status" | "atrasada">): [string, string] =>
  o.atrasada ? ["bad", `Atrasada · ${statusOrdemLabel(o.status)}`] : [statusOrdemTom(o.status), statusOrdemLabel(o.status)];

/** "18/11 14:05" a partir de ISO. */
export const hm = (iso: string | null | undefined): string => {
  if (!iso) return "—";
  const d = new Date(iso);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${p(d.getDate())}/${p(d.getMonth() + 1)} ${p(d.getHours())}:${p(d.getMinutes())}`;
};

export const minutosEntre = (a: string | null | undefined, b: string | null | undefined): number | null =>
  a && b ? Math.max(0, Math.round((Date.parse(b) - Date.parse(a)) / 60000)) : null;
