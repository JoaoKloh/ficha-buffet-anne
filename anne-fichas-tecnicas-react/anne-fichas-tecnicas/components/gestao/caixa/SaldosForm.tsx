"use client";

import { useState } from "react";
import { Carregando, FormSheet, Fld } from "@/components/ui/Gestao";
import { useDados } from "@/components/ui/Sheet";
import { CONTAS, type Conta } from "@/lib/models/dominio";
import type { FluxoCaixaResponseDTO } from "@/lib/models/caixa";
import { salvarSaldos, telaFluxo } from "@/lib/api/gestao/caixa";
import { dec, numIn } from "@/lib/utils/numero";
import { hoje, round2 } from "@/lib/utils/datas";

/** Saldo de cada conta no fim de um dia (do extrato). Sem os saldos atuais em mãos, busca no fluxo de caixa. */
export function SaldosForm({ contas, data }: { contas?: FluxoCaixaResponseDTO["contas"]; data?: string | null }) {
  const { dados, erro } = useDados(() => (contas ? Promise.resolve(null) : telaFluxo()), [!!contas]);
  if (contas) return <Formulario contas={contas} data={data ?? null} />;
  if (!dados) return <Carregando erro={erro} />;
  return <Formulario contas={dados.contas} data={dados.saldosData} />;
}

/** Os pagamentos e recebimentos registrados depois da data informada são somados ao saldo. */
function Formulario({ contas, data }: { contas: FluxoCaixaResponseDTO["contas"]; data: string | null }) {
  const inicial = Object.fromEntries(CONTAS.map(([k]) => [k, dec(contas.find((c) => c.conta === k)?.informado ?? "")])) as Record<Conta, string>;
  const [v, setV] = useState({ data: data ?? hoje(), valores: inicial });
  return (
    <FormSheet
      validar={() => (!v.data ? "Informe a data." : null)}
      onSalvar={() =>
        salvarSaldos({
          data: v.data,
          contas: Object.fromEntries(CONTAS.map(([k]) => [k, v.valores[k].trim() === "" ? null : round2(numIn(v.valores[k]))])) as Record<Conta, number | null>,
        })
      }
      okMsg="Saldos atualizados"
    >
      <Fld label="Saldo no fim do dia" obrig full hint="Use o saldo do extrato. Pagamentos e recebimentos registrados depois desta data são somados.">
        <input type="date" value={v.data} onChange={(e) => setV({ ...v, data: e.target.value })} />
      </Fld>
      {CONTAS.map(([k, rot]) => (
        <Fld key={k} label={`${rot} (R$)`}>
          <input inputMode="decimal" placeholder="0,00" value={v.valores[k]} onChange={(e) => setV({ ...v, valores: { ...v.valores, [k]: e.target.value } })} />
        </Fld>
      ))}
    </FormSheet>
  );
}
