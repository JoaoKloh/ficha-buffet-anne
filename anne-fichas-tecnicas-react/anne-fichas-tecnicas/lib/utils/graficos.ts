/** Marcas "redondas" do eixo, sempre incluindo o zero. */
export function marcas(min: number, max: number, alvo = 4, inteiro = false): number[] {
  if (max <= min) max = min + 1;
  const bruto = (max - min) / alvo;
  const pot = Math.pow(10, Math.floor(Math.log10(bruto)));
  let passo = [1, 2, 2.5, 5, 10].map((m) => m * pot).find((p) => p >= bruto) ?? 10 * pot;
  if (inteiro) passo = Math.max(1, Math.ceil(passo));
  const out: number[] = [];
  for (let v = Math.floor(min / passo) * passo; v <= max + passo * 1e-6; v += passo) out.push(Math.round(v * 1e6) / 1e6);
  if (out[out.length - 1]! < max) out.push(out[out.length - 1]! + passo);
  return out;
}
