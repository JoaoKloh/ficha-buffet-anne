"use client";

import type { Margens } from "@/lib/models/orcamento";
import { fmtCurrency, fmtCurrencySigned, fmtNumero, fmtPercent } from "@/lib/utils/format";
import type { ResumoOrcamento } from "@/lib/utils/orcamento";
import { OrcCard, Stat } from "./OrcamentoCampos";
import type { PainelProps } from "./OrcamentoPaineis";

const SLIDERS: { chave: keyof Margens; label: string; max: number }[] = [
  { chave: "margemBruta", label: "Margem de contribuição bruta", max: 90 },
  { chave: "comissao", label: "Comissão de venda", max: 30 },
  { chave: "outros", label: "Outros valores", max: 30 },
  { chave: "impostos", label: "Impostos", max: 30 },
  { chave: "despesasFixas", label: "Despesas fixas (% da venda)", max: 60 },
  { chave: "margemLucroBruto", label: "Margem de lucro bruto (% da venda)", max: 60 },
];

/** Barra única empilhada: onde vai cada real do preço de venda (antes de comissão/impostos). */
function GraficoComposicao({ resumo }: { resumo: ResumoOrcamento }) {
  const segmentos = [
    { label: "Alimentos", valor: resumo.alimentos, cor: "var(--serie-1)" },
    { label: "Equipe", valor: resumo.equipe, cor: "var(--serie-2)" },
    { label: "Louças & materiais", valor: resumo.materiais, cor: "var(--serie-3)" },
    { label: "Extras & degustação", valor: resumo.extras, cor: "var(--serie-4)" },
    { label: "Margem de contribuição", valor: Math.max(0, resumo.margemValor), cor: "var(--serie-5)" },
  ];
  const total = segmentos.reduce((s, x) => s + x.valor, 0) || 1;
  const largura = 720;
  const altura = 40;

  let x = 0;
  const barras = segmentos.map((s) => {
    const w = (s.valor / total) * largura;
    const inicio = x;
    x += w;
    return { ...s, x: inicio, w, pct: (s.valor / total) * 100 };
  });

  return (
    <>
      <svg
        className="orc-chart"
        viewBox={`0 0 ${largura} ${altura}`}
        preserveAspectRatio="none"
        role="img"
        aria-label="Composição do preço de venda"
      >
        {barras
          .filter((b) => b.w > 0.5)
          .map((b) => (
            // 2px de respiro entre os segmentos, cantos arredondados.
            <rect key={b.label} x={b.x} y={0} width={Math.max(0, b.w - 2)} height={altura} rx={4} fill={b.cor}>
              <title>{`${b.label}: ${fmtCurrency(b.valor)} (${b.pct.toFixed(0)}%)`}</title>
            </rect>
          ))}
      </svg>
      <ul className="orc-legend">
        {barras.map((b) => (
          <li key={b.label}>
            <span className="orc-legend-sw" style={{ background: b.cor }} aria-hidden="true" />
            {b.label} — {fmtCurrency(b.valor)} ({b.pct.toFixed(0)}%)
          </li>
        ))}
      </ul>
    </>
  );
}

function Slider({
  label,
  valor,
  max,
  onChange,
}: {
  label: string;
  valor: number;
  max: number;
  onChange: (v: number) => void;
}) {
  return (
    <label className="orc-slider">
      <span className="orc-slider-label">{label}</span>
      <span className="orc-slider-row">
        <input type="range" min={0} max={max} step={1} value={valor} onChange={(e) => onChange(Number(e.target.value))} />
        <span className="orc-slider-val">{fmtPercent(valor)}</span>
      </span>
    </label>
  );
}

export function PrecificacaoPainel({ orc, resumo, atualizar }: PainelProps) {
  const m = orc.margens;
  const pctVenda = (v: number) => fmtPercent(resumo.venda ? (v / resumo.venda) * 100 : 0);

  function setMargem(chave: keyof Margens, valor: number) {
    const novo = { ...m, [chave]: valor };
    // O investimento sai de dentro do lucro bruto: nunca pode ser maior que ele.
    if (novo.margemInvestimento > novo.margemLucroBruto) novo.margemInvestimento = novo.margemLucroBruto;
    atualizar({ margens: novo });
  }

  return (
    <>
      <OrcCard titulo="Composição do custo">
        <div className="orc-stats orc-mb">
          <Stat rotulo="Alimentos" valor={fmtCurrency(resumo.alimentos)} />
          <Stat rotulo="Equipe" valor={fmtCurrency(resumo.equipe)} />
          <Stat rotulo="Louças & materiais" valor={fmtCurrency(resumo.materiais)} />
          <Stat rotulo="Extras & degustação" valor={fmtCurrency(resumo.extras)} />
        </div>
        <GraficoComposicao resumo={resumo} />
      </OrcCard>

      <OrcCard
        titulo="Margens e repasses"
        dica="Cada camada é aplicada sobre o valor anterior: preço = custo ÷ (1 − %)"
      >
        <div className="orc-grid cols-2">
          {SLIDERS.map((s) => (
            <Slider
              key={s.chave}
              label={s.label}
              valor={m[s.chave]}
              max={s.max}
              onChange={(v) => setMargem(s.chave, v)}
            />
          ))}
          <Slider
            label="Margem de investimento (% da venda)"
            valor={m.margemInvestimento}
            max={m.margemLucroBruto}
            onChange={(v) => setMargem("margemInvestimento", v)}
          />
        </div>
      </OrcCard>

      <OrcCard titulo="Resultado da precificação">
        <div className="orc-stats">
          <Stat rotulo="Custo total do evento" valor={fmtCurrency(resumo.custo)} destaque />
          <Stat rotulo="Valor de venda sugerido" valor={fmtCurrency(resumo.venda)} destaque />
          <Stat rotulo="Valor por pessoa" valor={fmtCurrency(resumo.porPessoa)} destaque />
          <Stat rotulo="Margem de contribuição" valor={fmtCurrency(resumo.margemValor)} destaque />
        </div>
        <dl className="orc-dl orc-mt">
          <div className="fin-dre-row">
            <dt>Despesas fixas</dt>
            <dd>{fmtCurrency(resumo.despesas)}</dd>
          </div>
          <div className="fin-dre-row">
            <dt>Margem de lucro bruto</dt>
            <dd>{fmtCurrency(resumo.lucroBruto)}</dd>
          </div>
          <div className="fin-dre-row sub">
            <dt>↳ Margem de investimento</dt>
            <dd>{fmtCurrency(resumo.investimento)}</dd>
          </div>
          <div className="fin-dre-row sub">
            <dt>↳ Lucro</dt>
            <dd>{fmtCurrency(resumo.lucro)}</dd>
          </div>
        </dl>
      </OrcCard>

      <OrcCard titulo="Resultado final (estimado × realizado)">
        <div className="orc-stats">
          <Stat rotulo="Lucro estimado" valor={fmtCurrency(resumo.lucro)} />
          <Stat
            rotulo="Saldo dos insumos"
            valor={fmtCurrencySigned(resumo.saldoInsumos)}
            sinal={resumo.saldoInsumos}
          />
          <Stat
            rotulo="Serviços adicionais"
            valor={fmtCurrencySigned(resumo.servicosContrib)}
            sinal={resumo.servicosContrib}
          />
          <Stat
            rotulo="Pacote de bebidas"
            valor={fmtCurrencySigned(resumo.pacoteContrib)}
            sinal={resumo.pacoteContrib}
          />
        </div>
        <div className="orc-stats orc-mt">
          <Stat rotulo="Lucro real projetado" valor={fmtCurrency(resumo.lucroReal)} destaque />
        </div>
      </OrcCard>

      <OrcCard titulo="Indicadores">
        <div className="orc-stats">
          <Stat rotulo="CMV alimentos (real)" valor={pctVenda(resumo.alimentos)} />
          <Stat rotulo="Custo de equipe (real)" valor={pctVenda(resumo.equipe)} />
          <Stat rotulo="Louças & materiais (real)" valor={pctVenda(resumo.materiais)} />
          <Stat rotulo="Gramas por convidado" valor={`${fmtNumero(resumo.gramas)} g`} />
        </div>
      </OrcCard>

      <OrcCard titulo="Observações">
        <div className="field-block">
          <textarea
            aria-label="Observações do orçamento"
            value={orc.notas}
            placeholder="Justificativas, condições de pagamento, observações do orçamento…"
            onChange={(e) => atualizar({ notas: e.target.value })}
          />
        </div>
      </OrcCard>
    </>
  );
}
