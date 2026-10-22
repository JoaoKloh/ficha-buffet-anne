"use client";

import { useState } from "react";
import type { Lancamento } from "@/lib/models/financeiro";
import { fmtCurrency, fmtCurrencySigned, fmtPercent } from "@/lib/utils/format";
import {
  dreAnualTabulada,
  montarDre,
  rotuloPeriodo,
  situacaoReferencia,
  type Periodo,
  type Referencia,
} from "@/lib/utils/financeiro";

interface Props {
  /** Lançamentos do período selecionado. */
  lancamentos: Lancamento[];
  /** Todos os lançamentos: a cópia para o Excel usa o ano inteiro do período. */
  todos: Lancamento[];
  periodo: Periodo;
  /** Clique numa linha de categoria: abre a aba Lançamentos filtrada por ela. */
  onAbrirCategoria: (categoria: string) => void;
}

export function FinanceiroDre({ lancamentos, todos, periodo, onAbrirCategoria }: Props) {
  const [copiado, setCopiado] = useState(false);
  const [textoManual, setTextoManual] = useState<string | null>(null);
  const dre = montarDre(lancamentos);
  const rl = dre.receitaLiquida;
  const pct = (valor: number) => (rl ? fmtPercent((Math.abs(valor) / rl) * 100) : "");

  // Produções com receita vinculada no período (para ticket médio).
  const producoesComReceita = new Set(
    lancamentos.filter((l) => l.tipo === "receita").flatMap((l) => l.producaoIds ?? []),
  ).size;
  const margemContribuicao = rl > 0 ? dre.margemContribuicao / rl : NaN;
  const pontoEquilibrio = margemContribuicao > 0 ? dre.despesasFixas / margemContribuicao : NaN;

  const indicadores: [string, string][] = [
    ["Lançamentos no período", String(lancamentos.length)],
    ["Produções com receita vinculada", producoesComReceita ? String(producoesComReceita) : "–"],
    [
      "Receita média por produção",
      producoesComReceita ? fmtCurrency(dre.receitaBruta / producoesComReceita) : "–",
    ],
    ["Custo direto sobre a receita líquida", rl > 0 ? fmtPercent((dre.custosDiretos / rl) * 100) : "–"],
    ["Margem de contribuição", isFinite(margemContribuicao) ? fmtPercent(margemContribuicao * 100) : "–"],
    ["Margem líquida", rl > 0 ? fmtPercent((dre.resultado / rl) * 100) : "–"],
    ["Ponto de equilíbrio (receita líquida)", isFinite(pontoEquilibrio) ? fmtCurrency(pontoEquilibrio) : "–"],
  ];

  function copiar() {
    const texto = dreAnualTabulada(todos, periodo.ano);
    const ok = () => {
      setTextoManual(null);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2500);
    };
    // Sem acesso à área de transferência: mostra o texto para copiar à mão.
    navigator.clipboard?.writeText(texto).then(ok, () => setTextoManual(texto)) ?? setTextoManual(texto);
  }

  return (
    <div className="fin-stack">
      <section className="fin-panel fin-dre">
        <h2 className="section-h">DRE · {rotuloPeriodo(periodo)}</h2>
        <div className="fin-dre-tabela">
          {dre.linhas.map((linha, i) => {
            if (linha.tipo === "grupo") {
              return (
                <div key={i} className="fin-dre-linha grupo">
                  <span>
                    {linha.rotulo}
                    {linha.referencia && <RefChip referencia={linha.referencia} valor={linha.valor} rl={rl} />}
                  </span>
                  <span className="num">{fmtCurrencySigned(linha.valor)}</span>
                  <span className="pct" />
                </div>
              );
            }
            if (linha.tipo === "item") {
              return (
                <button
                  key={i}
                  type="button"
                  className="fin-dre-linha item"
                  onClick={() => onAbrirCategoria(linha.categoria)}
                  title={`Ver lançamentos de ${linha.categoria}`}
                >
                  <span>{linha.rotulo}</span>
                  <span className="num">{fmtCurrency(linha.valor)}</span>
                  <span className="pct">{pct(linha.valor)}</span>
                </button>
              );
            }
            const ehReceitaLiquida = linha.rotulo === "= Receita líquida";
            return (
              <div key={i} className={`fin-dre-linha total ${linha.final ? "final" : ""}`}>
                <span>
                  {linha.rotulo}
                  {linha.referencia && <RefChip referencia={linha.referencia} valor={linha.valor} rl={rl} />}
                </span>
                <span className={`num ${linha.valor < 0 ? "neg" : ""}`}>{fmtCurrencySigned(linha.valor)}</span>
                <span className="pct">{ehReceitaLiquida ? "" : pct(linha.valor)}</span>
              </div>
            );
          })}
        </div>
      </section>

      <section className="fin-panel">
        <h2 className="section-h">Indicadores</h2>
        <dl className="fin-dre-list">
          {indicadores.map(([rotulo, valor]) => (
            <div key={rotulo} className="fin-dre-row">
              <dt>{rotulo}</dt>
              <dd>{valor}</dd>
            </div>
          ))}
        </dl>
      </section>

      <div className="fin-copiar">
        <button type="button" className="btn ghost" onClick={copiar}>
          Copiar DRE de {periodo.ano} para o Excel
        </button>
        {copiado && <span className="fin-muted pos">Copiado.</span>}
      </div>
      {textoManual && (
        <textarea className="fin-copiar-texto" readOnly value={textoManual} aria-label="DRE para copiar" autoFocus />
      )}

      <div className="fin-notas fin-muted">
        <p>Clique numa linha de categoria da DRE para ver os lançamentos dela.</p>
        <p>
          <b>Competência:</b> o lançamento entra no mês da data inicial. Lance a receita na data do evento,
          mesmo que o sinal tenha sido recebido antes.
        </p>
        <p>
          <b>Simples Nacional:</b> o DAS vai em “Impostos sobre vendas”. IRPJ/CSLL só se a empresa for Lucro
          Presumido ou Real.
        </p>
        <p>As faixas de referência são aproximadas para buffets e variam com o padrão do serviço.</p>
      </div>
    </div>
  );
}

/** Faixa de referência; colorida (sage/âmbar/ferrugem) quando há receita líquida para comparar. */
function RefChip({ referencia, valor, rl }: { referencia: Referencia; valor: number; rl: number }) {
  const situacao = rl > 0 ? situacaoReferencia(referencia, valor / rl) : "";
  return <span className={`fin-ref ${situacao}`}>ref. {referencia[2]}</span>;
}
