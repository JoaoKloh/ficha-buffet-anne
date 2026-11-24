import { RANK_STATUS, STATUS_EVENTO, TIPOS_EVENTO, rotulo, type StatusEvento } from "@/lib/models/dominio";

/** Tom visual da etapa do evento, como no protótipo. */
export function stTone(k: StatusEvento): string {
  if (k === "perdido" || k === "cancelado") return "bad";
  if (k === "acertado") return "good";
  if (k === "realizado") return "brass";
  if (k === "degustacao") return "deg";
  if (k === "cardapio_fechado") return "fech";
  return RANK_STATUS[k] >= 3 ? "accent" : "";
}
export const stLabel = (k: StatusEvento) => rotulo(STATUS_EVENTO, k);
export const tipoLabel = (k: string) => rotulo(TIPOS_EVENTO, k, "Evento");
