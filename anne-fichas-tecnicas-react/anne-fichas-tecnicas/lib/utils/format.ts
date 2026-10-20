export function fmtDate(iso: string | undefined | null): string {
  if (!iso) return "Data a combinar";
  const [y, m, d] = iso.split("-");
  if (!y || !m || !d) return iso;
  return `${d}/${m}/${y}`;
}

export function fmtQuantidade(value: number): string {
  return value % 1 === 0 ? String(value) : value.toFixed(2);
}

export function fmtCurrency(value: number | undefined | null): string {
  const num = typeof value === "number" && !isNaN(value) ? value : 0;
  return num.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

/** Moeda com sinal "−" explícito para negativos (saldos, contribuições). */
export function fmtCurrencySigned(value: number): string {
  return (value < 0 ? "−" : "") + fmtCurrency(Math.abs(value));
}

export function fmtPercent(value: number): string {
  return `${value.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;
}

export function fmtNumero(value: number): string {
  return value.toLocaleString("pt-BR", { maximumFractionDigits: 1 });
}
