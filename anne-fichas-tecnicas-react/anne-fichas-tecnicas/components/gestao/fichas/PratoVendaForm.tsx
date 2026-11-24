"use client";

import { useState } from "react";
import { Carregando, FormSheet, Fld, Sel } from "@/components/ui/Gestao";
import { useDados } from "@/components/ui/Sheet";
import { useAoVivo } from "@/components/ui/useAoVivo";
import { META_CMV, SECOES_CARTA, type SecaoCarta } from "@/lib/models/dominio";
import type { PratoVendaResponseDTO } from "@/lib/models/pratoVenda";
import { atualizarPratoVenda, calcularCmv, criarPratoVenda, excluirPratoVenda, opcoesFichas } from "@/lib/api/gestao/fichas";
import { brl, dec, numIn, pct } from "@/lib/utils/numero";
import { round2 } from "@/lib/utils/datas";

const tomCmv = (r: number | null) => (r == null ? "" : r <= META_CMV ? "pos" : r <= META_CMV + 0.06 ? "" : "neg");

/** Prato da carta: fichas que leva, insumos diretos, preço e CMV ao vivo. */
export function PratoVendaForm({ prato }: { prato?: PratoVendaResponseDTO }) {
  const { dados: op, erro } = useDados(opcoesFichas);
  const [v, setV] = useState({ nome: prato?.nome ?? "", secao: (prato?.secao ?? "campo") as SecaoCarta, preco: dec(prato?.preco || ""), codigoPdv: prato?.codigoPdv ?? "", ativo: prato?.ativo !== false, obs: prato?.obs ?? "" });
  const [comps, setComps] = useState(() => {
    const l = (prato?.componentes ?? []).map((x) => ({ preparacaoId: String(x.preparacaoId), qtd: dec(x.qtd) }));
    return l.length ? l : [{ preparacaoId: "", qtd: "" }];
  });
  const [extras, setExtras] = useState((prato?.extras ?? []).map((x) => ({ insumoId: String(x.insumoId), qtd: dec(x.qtd) })));
  const toComp = () => comps.filter((x) => x.preparacaoId).map((x) => ({ preparacaoId: Number(x.preparacaoId), qtd: numIn(x.qtd) }));
  const toExtras = () => extras.filter((x) => x.insumoId).map((x) => ({ insumoId: Number(x.insumoId), qtd: numIn(x.qtd) }));
  const cmv = useAoVivo(() => calcularCmv({ componentes: toComp(), extras: toExtras(), preco: numIn(v.preco) }), JSON.stringify([toComp(), toExtras(), numIn(v.preco)]));
  if (!op) return <Carregando erro={erro} />;
  const prep = (id: string) => op.preparacoes.find((p) => String(p.id) === id);
  const dto = () => ({ nome: v.nome.trim(), secao: v.secao, preco: round2(numIn(v.preco)), codigoPdv: v.codigoPdv.trim(), ativo: v.ativo, obs: v.obs, componentes: toComp(), extras: toExtras() });

  return (
    <FormSheet
      validar={() => {
        const c = toComp(), x = toExtras();
        return !v.nome.trim() ? "Informe o nome do prato." : !c.length && !x.length ? "Ligue pelo menos uma ficha técnica ou insumo ao prato." : c.some((k) => !(k.qtd > 0)) || x.some((k) => !(k.qtd > 0)) ? "Informe a quantidade por prato de cada item." : numIn(v.preco) < 0 ? "O preço não pode ser negativo." : null;
      }}
      onSalvar={() => (prato ? atualizarPratoVenda({ ...dto(), id: prato.id }) : criarPratoVenda(dto()))}
      okMsg="Prato salvo"
      submit="Salvar prato"
      onExcluir={prato ? () => excluirPratoVenda(prato.id) : null}
      excluirLabel="Excluir prato"
      excluirConfirma="Excluir este prato do restaurante?"
    >
      <Fld label="Prato" obrig full>
        <input value={v.nome} onChange={(e) => setV({ ...v, nome: e.target.value })} placeholder="Ex.: Nhoque de baroa com ragu de costela" autoFocus />
      </Fld>
      <Fld label="Seção da carta">
        <Sel value={v.secao} onChange={(x) => setV({ ...v, secao: x as SecaoCarta })} opcoes={SECOES_CARTA} />
      </Fld>
      <Fld label="Preço de venda (R$)">
        <input inputMode="decimal" placeholder="0,00" value={v.preco} onChange={(e) => setV({ ...v, preco: e.target.value })} />
      </Fld>
      <Fld label="Código no PDV" hint="Liga as vendas do PDV a este prato na importação.">
        <input value={v.codigoPdv} onChange={(e) => setV({ ...v, codigoPdv: e.target.value })} placeholder="Opcional" />
      </Fld>
      <label className="check fld full">
        <input type="checkbox" checked={v.ativo} onChange={(e) => setV({ ...v, ativo: e.target.checked })} /> Está na carta
      </label>
      <div className="fld full">
        <span>Fichas técnicas que o prato leva</span>
        <div className="fitems">
          {comps.map((x, ix) => (
            <div key={ix} className="fitem two">
              <Fld label="Ficha técnica">
                <Sel
                  value={x.preparacaoId}
                  onChange={(id) => setComps((l) => l.map((c, i) => (i === ix ? { preparacaoId: id, qtd: c.qtd || dec(prep(id)?.porcaoPadrao || 1) } : c)))}
                  grupos={op.preparacoesAgrupadas.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))}
                  vazio="Escolha…"
                />
              </Fld>
              <Fld label={`Por prato${prep(x.preparacaoId) ? ` (${prep(x.preparacaoId)!.unidade})` : ""}`}>
                <input inputMode="decimal" value={x.qtd} onChange={(e) => setComps((l) => l.map((c, i) => (i === ix ? { ...c, qtd: e.target.value } : c)))} />
              </Fld>
              <button type="button" className="btn sm ghost" aria-label="Remover" onClick={() => setComps((l) => (l.length > 1 ? l.filter((_, i) => i !== ix) : [{ preparacaoId: "", qtd: "" }]))}>
                ✕
              </button>
            </div>
          ))}
        </div>
        <div>
          <button type="button" className="btn sm" onClick={() => setComps((l) => [...l, { preparacaoId: "", qtd: "" }])}>
            Adicionar ficha
          </button>
        </div>
        <small>Quantidade de cada preparação num prato, na unidade da ficha: 1 porção, 0,18 kg de nhoque, 0,12 l de molho.</small>
      </div>
      <div className="fld full">
        <span>Insumos diretos na montagem</span>
        <div className="fitems">
          {extras.map((x, ix) => (
            <div key={ix} className="fitem two">
              <Fld label="Insumo direto">
                <Sel value={x.insumoId} onChange={(id) => setExtras((l) => l.map((c, i) => (i === ix ? { ...c, insumoId: id } : c)))} grupos={op.insumos.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))} vazio="Escolha…" />
              </Fld>
              <Fld label={`Por prato${x.insumoId ? ` (${op.unidades[Number(x.insumoId)] ?? ""})` : ""}`}>
                <input inputMode="decimal" value={x.qtd} onChange={(e) => setExtras((l) => l.map((c, i) => (i === ix ? { ...c, qtd: e.target.value } : c)))} />
              </Fld>
              <button type="button" className="btn sm ghost" aria-label="Remover" onClick={() => setExtras((l) => l.filter((_, i) => i !== ix))}>
                ✕
              </button>
            </div>
          ))}
        </div>
        <div>
          <button type="button" className="btn sm" onClick={() => setExtras((l) => [...l, { insumoId: "", qtd: "" }])}>
            Adicionar insumo
          </button>
        </div>
        <small>O que vai no prato sem ficha própria: ervas, parmesão ralado na hora, embalagem de viagem.</small>
      </div>
      <p className="note" style={{ gridColumn: "1/-1" }}>
        {cmv ? (
          <>
            Custo do prato <b>{brl(cmv.custo)}</b>
            {cmv.cmv != null && (
              <>
                {" "}
                · CMV <b className={tomCmv(cmv.cmv)}>{pct(cmv.cmv)}</b>
              </>
            )}
            {cmv.custo > 0 && (
              <>
                {" "}
                · preço para CMV de {pct(META_CMV)}: <b>{brl(cmv.custo / META_CMV)}</b>
              </>
            )}
            {cmv.faltando > 0 && <span className="neg"> · {cmv.faltando} ite{cmv.faltando > 1 ? "ns" : "m"} sem preço</span>}
          </>
        ) : (
          "Calculando custo…"
        )}
      </p>
      <Fld label="Observações de montagem" full>
        <textarea value={v.obs} onChange={(e) => setV({ ...v, obs: e.target.value })} />
      </Fld>
    </FormSheet>
  );
}
