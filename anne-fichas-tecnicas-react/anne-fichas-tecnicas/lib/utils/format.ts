export function fmtDate(iso: string | undefined | null): string {
  if (!iso) return "Data a combinar";
  const [y, m, d] = iso.split("-");
  if (!y || !m || !d) return iso;
  return `${d}/${m}/${y}`;
}

export function fmtQuantidade(value: number): string {
  return value % 1 === 0 ? String(value) : value.toFixed(2);
}
