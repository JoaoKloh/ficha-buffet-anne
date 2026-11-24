"use client";

import { useState } from "react";
import { FormSheet, Fld, Sel } from "@/components/ui/Gestao";
import { CONTAS, FORMAS_RECEBIMENTO, type Conta, type FormaRecebimento } from "@/lib/models/dominio";
import type { EventoResponseDTO, ExtraResponseDTO, ParcelaResponseDTO } from "@/lib/models/evento";
import { atualizarExtra, atualizarParcela, criarExtra, excluirExtra, excluirParcela } from "@/lib/api/gestao/eventos";
import { brl, dec, numIn } from "@/lib/utils/numero";
import { hoje, round2 } from "@/lib/utils/datas";

/** Parcela do contrato: vencimento, valor e recebimento (atualiza a receita ligada). */
export function ParcelaForm({ parcela, conta }: { parcela: ParcelaResponseDTO; conta: Conta }) {
  const parcial = !parcela.pagoEm && parcela.recebido > 0;
  const [v, setV] = useState({
    vencimento: parcela.vencimento,
    valor: dec(parcela.valor),
    pago: !!parcela.pagoEm,
    pagoEm: parcela.pagoEm ?? hoje(),
    forma: parcela.forma as FormaRecebimento | "",
    conta,
  });
  return (
    <FormSheet
      validar={() => (!(numIn(v.valor) > 0) ? "Informe o valor." : null)}
      onSalvar={() =>
        atualizarParcela({ id: parcela.id, vencimento: v.vencimento, valor: round2(numIn(v.valor)), pagoEm: v.pago ? v.pagoEm || hoje() : null, forma: v.pago ? v.forma : "", conta: v.conta })
      }
      onExcluir={() => excluirParcela(parcela.id)}
      excluirLabel="Excluir parcela"
    >
      <Fld label="Vencimento" obrig>
        <input type="date" value={v.vencimento} onChange={(e) => setV({ ...v, vencimento: e.target.value })} />
      </Fld>
      <Fld label="Valor (R$)" obrig>
        <input inputMode="decimal" value={v.valor} onChange={(e) => setV({ ...v, valor: e.target.value })} />
      </Fld>
      <label className="check fld full">
        <input type="checkbox" checked={v.pago} onChange={(e) => setV({ ...v, pago: e.target.checked })} /> Recebida por inteiro
      </label>
      {parcial && (
        <p className="note" style={{ gridColumn: "1/-1", marginTop: -8 }}>
          Já recebido em parte: {brl(parcela.recebido)}. Para receber outra parte, use Receber em Caixa › Contas a receber.
        </p>
      )}
      <Fld label="Recebida em" hidden={!v.pago}>
        <input type="date" value={v.pagoEm} onChange={(e) => setV({ ...v, pagoEm: e.target.value })} />
      </Fld>
      <Fld label="Forma" hidden={!v.pago}>
        <Sel value={v.forma} onChange={(x) => setV({ ...v, forma: x as FormaRecebimento })} opcoes={FORMAS_RECEBIMENTO} vazio="—" />
      </Fld>
      <Fld label="Conta" full hidden={!v.pago}>
        <Sel value={v.conta} onChange={(x) => setV({ ...v, conta: x as Conta })} opcoes={CONTAS} />
      </Fld>
    </FormSheet>
  );
}

/** Extra do evento (convidados acima do contratado, hora extra...), que vira receita prevista. */
export function ExtraForm({ evento, extra }: { evento: EventoResponseDTO; extra?: ExtraResponseDTO }) {
  const [v, setV] = useState({
    descricao: extra?.descricao ?? "",
    quantidade: dec(extra?.quantidade ?? ""),
    valorUnitario: dec(extra?.valorUnitario ?? (evento.contrato?.valorConvidadoExtra || "")),
  });
  const dto = () => ({ eventoId: evento.id, descricao: v.descricao.trim(), quantidade: numIn(v.quantidade), valorUnitario: round2(numIn(v.valorUnitario)) });
  return (
    <FormSheet
      intro="Extras viram receita prevista do evento. Ex.: convidados acima do contratado, hora extra, estação adicional."
      validar={() => (!v.descricao.trim() ? "Descreva o extra." : !(numIn(v.quantidade) > 0 && numIn(v.valorUnitario) > 0) ? "Informe quantidade e valor." : null)}
      onSalvar={() => (extra ? atualizarExtra({ ...dto(), id: extra.id }) : criarExtra(dto()))}
      onExcluir={extra ? () => excluirExtra(extra.id) : null}
    >
      <Fld label="Descrição" obrig full>
        <input value={v.descricao} onChange={(e) => setV({ ...v, descricao: e.target.value })} placeholder="Convidados extras" autoFocus />
      </Fld>
      <Fld label="Quantidade" obrig>
        <input inputMode="decimal" value={v.quantidade} onChange={(e) => setV({ ...v, quantidade: e.target.value })} />
      </Fld>
      <Fld label="Valor unitário (R$)" obrig>
        <input inputMode="decimal" value={v.valorUnitario} onChange={(e) => setV({ ...v, valorUnitario: e.target.value })} />
      </Fld>
    </FormSheet>
  );
}
