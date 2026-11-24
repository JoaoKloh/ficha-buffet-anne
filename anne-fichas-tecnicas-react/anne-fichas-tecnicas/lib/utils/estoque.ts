import { unCurto } from "@/lib/models/dominio";
import type { MovimentoResponseDTO } from "@/lib/models/estoque";

/** Origem do movimento para exibir na lista (protótipo: origemLabel). */
export function origemMovimento(m: Pick<MovimentoResponseDTO, "tipo" | "origem" | "virtual" | "eventoId"> & { unidadeUso?: MovimentoResponseDTO["unidadeUso"] }): string {
  if (m.virtual) return m.origem === "ordem" ? "Ordem de produção" : m.eventoId ? "Consumo em evento (produção)" : "Produção de pré-preparo";
  if (m.tipo === "consumo") return `Consumo · ${unCurto(m.unidadeUso)}`;
  if (m.tipo === "sobra") return "Sobra de evento";
  if (m.tipo === "perda" && m.origem !== "checklist") return "Perda ou descarte";
  if (m.tipo === "devolucao") return "Devolução ao fornecedor";
  return (
    { nf: "Compra recebida (NF)", manual: "Lançamento manual", inventario: "Inventário", checklist: "Checklist do evento", consumo: "Consumo da unidade", ordem: "Ordem de produção", lote: "Lote de pré-preparo" }[m.origem] ??
    "Origem não informada"
  );
}
