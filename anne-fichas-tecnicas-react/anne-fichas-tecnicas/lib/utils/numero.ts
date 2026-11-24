/** Entrada numérica em pt-BR: "1.234,5" ou "1234.5" viram número; vazio vira 0. */
export function numIn(v: unknown): number {
  const s = String(v ?? "").trim();
  if (!s) return 0;
  if (s.includes(",")) {
    const n = Number(s.replace(/\./g, "").replace(",", "."));
    return isFinite(n) ? n : 0;
  }
  const n = Number(s);
  return isFinite(n) ? n : 0;
}

/** Número (ou null) para texto de campo com vírgula decimal. */
export const dec = (v: number | string | null | undefined): string => (v == null || v === "" ? "" : String(v).replace(".", ","));

export const brl = (n: number | null | undefined): string =>
  (Number(n) || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export const pct = (n: number | null | undefined): string =>
  n != null && isFinite(n) ? `${(n * 100).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%` : "—";

export const fmtN = (n: number | null | undefined): string => (Number(n) || 0).toLocaleString("pt-BR", { maximumFractionDigits: 3 });

const INTEIRAS = ["un", "dz", "maço", "garrafa", "botijão"];
export function fmtQ(n: number, u: string): string {
  if (!isFinite(n)) return "—";
  if (INTEIRAS.includes(u)) return String(Math.ceil(n - 1e-9));
  return n.toLocaleString("pt-BR", { maximumFractionDigits: n < 10 ? 2 : 1 });
}
export const qtdU = (n: number, u: string): string => `${fmtQ(Math.abs(n), u)} ${u || ""}`.trim();

/** Valor curto para eixo de gráfico: "R$ 12 mil". */
export function brlK(v: number): string {
  const a = Math.abs(v);
  const n = (x: number, casas: number) => x.toLocaleString("pt-BR", { maximumFractionDigits: casas });
  const corpo = a >= 1e9 ? n(a / 1e9, a >= 1e10 ? 0 : 1) + " bi" : a >= 1e6 ? n(a / 1e6, a >= 1e7 ? 0 : 1) + " mi" : a >= 1000 ? n(a / 1000, a >= 10000 ? 0 : 1) + " mil" : n(a, 0);
  return (v < 0 ? "−" : "") + "R$ " + corpo;
}

export const MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];
export const MESES_L = ["janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"];
export const fdate = (iso: string | null | undefined): string => (iso ? `${iso.slice(8, 10)}/${iso.slice(5, 7)}/${iso.slice(0, 4)}` : "—");
export const ymLabel = (ym: string): string => `${MESES_L[Number(ym.slice(5, 7)) - 1] ?? ""} ${ym.slice(0, 4)}`;
export const capitalizar = (s: string): string => s.charAt(0).toUpperCase() + s.slice(1);
export const mesCurto = (ym: string): string => `${MESES[Number(ym.slice(5, 7)) - 1]}/${ym.slice(2, 4)}`;
const DIAS_SEM = ["dom", "seg", "ter", "qua", "qui", "sex", "sáb"];
export const dLabel = (iso: string): string => `${DIAS_SEM[new Date(iso + "T12:00:00").getDay()]} ${iso.slice(8, 10)}/${iso.slice(5, 7)}`;
export const plural = (n: number, um: string, varios: string): string => `${n} ${n === 1 ? um : varios}`;
