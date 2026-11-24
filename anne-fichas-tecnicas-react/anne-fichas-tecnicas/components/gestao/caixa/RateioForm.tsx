"use client";

import { useState } from "react";
import { FormSheet, Fld } from "@/components/ui/Gestao";
import { UN_OPER, unLabel, type UnidadeOperacional } from "@/lib/models/dominio";
import { salvarRateio } from "@/lib/api/gestao/caixa";
import { dec, fmtN, numIn } from "@/lib/utils/numero";
import { round2 } from "@/lib/utils/datas";

/** Como dividir entre as unidades os custos da estrutura comum. Os percentuais somam 100%. */
export function RateioForm({ rateio }: { rateio: Record<UnidadeOperacional, number> }) {
  const [v, setV] = useState(Object.fromEntries(UN_OPER.map((u) => [u, dec(Math.round((rateio[u] ?? 0) * 10) / 10)])) as Record<UnidadeOperacional, string>);
  const total = UN_OPER.reduce((s, u) => s + numIn(v[u]), 0);
  return (
    <FormSheet
      intro="Como dividir entre as unidades os custos da estrutura comum (fábrica, aluguel, salários da produção, energia). Use percentuais que reflitam o uso real: área, horas de produção ou faturamento. Somam 100%."
      validar={() => (Math.abs(total - 100) > 0.5 ? `Os percentuais somam ${fmtN(round2(total))}%: ajuste para 100%.` : null)}
      onSalvar={() => salvarRateio({ buffet: round2(numIn(v.buffet)), restaurante: round2(numIn(v.restaurante)), chocolates: round2(numIn(v.chocolates)) })}
      okMsg="Rateio salvo"
    >
      {UN_OPER.map((u) => (
        <Fld key={u} label={`${unLabel(u)} (%)`}>
          <input inputMode="decimal" value={v[u]} onChange={(e) => setV({ ...v, [u]: e.target.value })} />
        </Fld>
      ))}
      <p className="note" style={{ gridColumn: "1/-1" }}>
        Soma: <b className={Math.abs(total - 100) > 0.5 ? "neg" : "pos"}>{fmtN(round2(total))}%</b>
      </p>
    </FormSheet>
  );
}
