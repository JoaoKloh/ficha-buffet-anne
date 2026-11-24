/** Datas em "AAAA-MM-DD" sem passar por UTC, como no protótipo. */

export const toIso = (d: Date): string =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

export const hoje = (): string => toIso(new Date());

export function addDays(iso: string, n: number): string {
  const d = new Date(iso + "T12:00:00");
  d.setDate(d.getDate() + n);
  return toIso(d);
}

export function addMonths(iso: string, n: number): string {
  const [a, m, dia] = iso.split("-").map(Number) as [number, number, number];
  const t = new Date(a, m - 1 + n, 1);
  const ultimo = new Date(t.getFullYear(), t.getMonth() + 1, 0).getDate();
  t.setDate(Math.min(dia, ultimo));
  return toIso(t);
}

/** a − b em dias. */
export const diffDays = (a: string, b: string): number =>
  Math.round((new Date(a + "T12:00:00").getTime() - new Date(b + "T12:00:00").getTime()) / 864e5);

/** Segunda-feira da semana de `iso`. */
export function segunda(iso: string): string {
  const d = new Date(iso + "T12:00:00");
  return addDays(iso, -((d.getDay() + 6) % 7));
}

export const round2 = (n: number): number => Math.round(n * 100) / 100;
export const r3 = (n: number): number => Math.round((Number(n) || 0) * 1000) / 1000;
export const sum = <T>(a: readonly T[], f: (x: T) => number | null | undefined): number =>
  a.reduce((s, x) => s + (Number(f(x)) || 0), 0);
