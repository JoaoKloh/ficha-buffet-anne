"use client";

import { useState } from "react";
import { Carregando, FormSheet, Fld, Sel } from "@/components/ui/Gestao";
import { useDados } from "@/components/ui/Sheet";
import { UNIDADES, type Unidade } from "@/lib/models/dominio";
import type { LancamentoResponseDTO } from "@/lib/models/caixa";
import { atualizarTransferencia, criarTransferencia, excluirLancamento } from "@/lib/api/gestao/caixa";
import { opcoesEstoque } from "@/lib/api/gestao/estoque";
import { brl, dec, numIn } from "@/lib/utils/numero";
import { hoje, round2 } from "@/lib/utils/datas";

/**
 * Transferência entre unidades: uma unidade usou produto ou insumo comprado por
 * outra. O custo sai da origem e entra no destino; o caixa e o consolidado não mudam.
 * Com item do cadastro e quantidade, o custo é calculado pelo último preço pago.
 */
export function TransferenciaForm({ transferencia }: { transferencia?: LancamentoResponseDTO }) {
  const { dados: op, erro } = useDados(() => opcoesEstoque("transferencia"));
  const t = transferencia;
  const [v, setV] = useState({
    data: t?.dataCompetencia ?? hoje(),
    responsavel: t?.responsavel ?? "",
    unidadeOrigem: (t?.unidadeOrigem ?? "buffet") as Unidade,
    unidadeDestino: (t?.unidadeDestino ?? "restaurante") as Unidade,
    insumoId: t?.insumoId ? String(t.insumoId) : "",
    item: t?.item ?? "",
    qtd: dec(t?.qtd ?? ""),
    unid: t?.unid ?? "",
    valor: dec(t?.valor ?? ""),
    categoriaId: t ? String(t.categoriaId) : "",
    obs: t?.obs ?? "",
  });
  if (!op) return <Carregando erro={erro} />;
  const cat = v.categoriaId || (op.categoriaAB ? String(op.categoriaAB) : "");
  const info = v.insumoId ? op.itensInfo[Number(v.insumoId)] : undefined;
  // Item do cadastro com preço: a quantidade calcula o custo.
  const recalcular = (insumoId: string, qtd: string, base: typeof v) => {
    const i = insumoId ? op.itensInfo[Number(insumoId)] : undefined;
    const q = numIn(qtd);
    return {
      ...base,
      insumoId,
      qtd,
      unid: i?.unidade || base.unid,
      item: base.item || (i ? i.nome : ""),
      valor: i && q && i.custo > 0 ? dec(round2(q * i.custo).toFixed(2)) : base.valor,
    };
  };
  const dto = () => ({
    data: v.data,
    responsavel: v.responsavel.trim(),
    unidadeOrigem: v.unidadeOrigem,
    unidadeDestino: v.unidadeDestino,
    insumoId: v.insumoId ? Number(v.insumoId) : null,
    item: (v.item || info?.nome || "").trim(),
    qtd: numIn(v.qtd) || null,
    unid: v.unid.trim(),
    valor: round2(numIn(v.valor)),
    categoriaId: Number(cat),
    obs: v.obs.trim(),
  });

  return (
    <FormSheet
      intro="Registre quando uma unidade usar produto ou insumo de outra. O custo sai da origem e entra no destino; o caixa e o resultado consolidado não mudam."
      validar={() =>
        v.unidadeOrigem === v.unidadeDestino
          ? "Origem e destino precisam ser unidades diferentes."
          : !(v.item.trim() || v.insumoId)
            ? "Diga o que foi transferido."
            : !(numIn(v.valor) > 0)
              ? "Informe o custo da transferência."
              : !cat
                ? "Escolha a linha de custo."
                : null
      }
      onSalvar={() => (t ? atualizarTransferencia({ ...dto(), id: t.id }) : criarTransferencia(dto()))}
      okMsg="Transferência registrada"
      onExcluir={t ? () => excluirLancamento(t.id) : null}
      excluirLabel="Excluir transferência"
      excluirConfirma="Excluir esta transferência? O custo volta para a unidade de origem."
    >
      <Fld label="Data" obrig>
        <input type="date" value={v.data} onChange={(e) => setV({ ...v, data: e.target.value })} />
      </Fld>
      <Fld label="Responsável">
        <input value={v.responsavel} onChange={(e) => setV({ ...v, responsavel: e.target.value })} placeholder="Quem enviou ou recebeu" />
      </Fld>
      <Fld label="Sai de">
        <Sel value={v.unidadeOrigem} onChange={(x) => setV({ ...v, unidadeOrigem: x as Unidade })} opcoes={UNIDADES} />
      </Fld>
      <Fld label="Vai para">
        <Sel value={v.unidadeDestino} onChange={(x) => setV({ ...v, unidadeDestino: x as Unidade })} opcoes={UNIDADES} />
      </Fld>
      <Fld label="Item do cadastro" full>
        <Sel
          value={v.insumoId}
          onChange={(x) => setV(recalcular(x, v.qtd, { ...v, item: x ? (op.itensInfo[Number(x)]?.nome ?? v.item) : v.item }))}
          grupos={op.itens.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))}
          vazio="Outro (descrever abaixo)"
        />
      </Fld>
      <Fld label="Produto ou insumo" full>
        <input value={v.item} onChange={(e) => setV({ ...v, item: e.target.value })} placeholder="Ex.: molho de tomate da fábrica, ganache, pães" />
      </Fld>
      <Fld label="Quantidade" hint={info ? (info.custo > 0 ? `Custo atual: ${brl(info.custo)} /${info.unidade}` : "Item sem preço no cadastro") : undefined}>
        <input inputMode="decimal" value={v.qtd} onChange={(e) => setV(recalcular(v.insumoId, e.target.value, v))} />
      </Fld>
      <Fld label="Unidade de medida">
        <input value={v.unid} onChange={(e) => setV({ ...v, unid: e.target.value })} placeholder="kg, un, l" />
      </Fld>
      <Fld label="Custo de transferência (R$)" obrig hint="A preço de custo. Com item do cadastro e quantidade, é calculado pelo último preço pago.">
        <input inputMode="decimal" placeholder="0,00" value={v.valor} onChange={(e) => setV({ ...v, valor: e.target.value })} />
      </Fld>
      <Fld label="Linha de custo">
        <Sel
          value={cat}
          onChange={(x) => setV({ ...v, categoriaId: x })}
          grupos={op.categoriasDespesa.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))}
          vazio="Escolha…"
        />
      </Fld>
      <Fld label="Observação" full>
        <input value={v.obs} onChange={(e) => setV({ ...v, obs: e.target.value })} />
      </Fld>
    </FormSheet>
  );
}
