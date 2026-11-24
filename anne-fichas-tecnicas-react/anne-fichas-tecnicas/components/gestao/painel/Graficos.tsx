"use client";

import { useEffect, useRef, useState } from "react";
import { marcas } from "@/lib/utils/graficos";
import { brl, brlK, pct } from "@/lib/utils/numero";

/**
 * Gráficos do Painel em SVG, com as cores do tema (creme, verde e dourado).
 * Recebem as séries prontas do BFF: aqui só se desenha.
 */

export const COR = { verde: "var(--green)", rosa: "var(--pink)", dourado: "var(--gold)", azul: "var(--blue)", ameixa: "var(--plum)", neutro: "var(--muted)" } as const;

function useLargura() {
  const ref = useRef<HTMLDivElement>(null);
  const [w, setW] = useState(0);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const ro = new ResizeObserver((es) => setW(Math.round(es[0]?.contentRect.width ?? 0)));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);
  return [ref, w] as const;
}

export interface Serie {
  rotulo: string;
  valores: number[];
  cor: string;
}

const ALT = 210;

/**
 * Barras (uma ou mais séries lado a lado) ou linha com área, por mês.
 * `atual` é a posição do mês corrente: depois dela é futuro e sai esmaecido
 * (barras) ou tracejado (linha).
 */
export function GraficoMeses({ titulo, rotulos, series, tipo, atual, dinheiro }: { titulo: string; rotulos: string[]; series: Serie[]; tipo: "barras" | "linha"; atual?: number; dinheiro?: boolean }) {
  const [ref, w] = useLargura();
  const [hover, setHover] = useState<number | null>(null);
  const n = rotulos.length;
  const fmt = (v: number) => (dinheiro ? brl(v) : String(v));
  const todos = series.flatMap((s) => s.valores);
  const ticks = marcas(Math.min(0, ...todos), Math.max(0, ...todos, dinheiro ? 0 : 1), 4, !dinheiro);
  const y0 = ticks[0]!;
  const y1 = ticks[ticks.length - 1]!;
  // A margem esquerda acompanha o maior rótulo do eixo, para ele nunca sair do cartão.
  const eixo = (t: number) => (dinheiro ? brlK(t) : String(t));
  const m = { l: Math.max(26, Math.max(...ticks.map((t) => eixo(t).length)) * 6.4 + 12), r: 6, t: 8, b: 22 };
  const pw = Math.max(0, w - m.l - m.r);
  const ph = ALT - m.t - m.b;
  const passo = n ? pw / n : 0;
  const X = (i: number) => m.l + passo * (i + 0.5);
  const Y = (v: number) => m.t + ph * (1 - (v - y0) / (y1 - y0));
  const cada = Math.max(1, Math.ceil(46 / Math.max(1, passo)));
  const futuro = (i: number) => atual != null && i > atual;
  const lb = Math.min(14, Math.max(3, (passo * 0.62) / series.length));
  const linha = series[0]!;

  return (
    <div ref={ref} style={{ position: "relative", height: "100%" }} onMouseLeave={() => setHover(null)}>
      {w > 0 && (
        <svg role="img" aria-label={titulo} viewBox={`0 0 ${w} ${ALT}`}>
          {ticks.map((t) => (
            <g key={t}>
              <line x1={m.l} x2={w - m.r} y1={Y(t)} y2={Y(t)} style={{ stroke: t === 0 ? "var(--muted)" : "var(--line)", strokeWidth: 1, opacity: t === 0 ? 0.5 : 1 }} />
              <text x={m.l - 8} y={Y(t) + 4} textAnchor="end" style={{ fill: "var(--muted)", fontSize: 11 }}>
                {eixo(t)}
              </text>
            </g>
          ))}
          {rotulos.map((r, i) =>
            i % cada === 0 ? (
              <text key={r} x={X(i)} y={ALT - 6} textAnchor="middle" style={{ fill: "var(--muted)", fontSize: 11, fontWeight: i === atual ? 700 : 400 }}>
                {r}
              </text>
            ) : null
          )}
          {tipo === "barras" &&
            series.map((s, si) =>
              s.valores.map((v, i) => {
                const x = X(i) - (lb * series.length + 2 * (series.length - 1)) / 2 + si * (lb + 2);
                const h = Math.abs(Y(v) - Y(0));
                return v ? <rect key={`${si}-${i}`} x={x} y={Math.min(Y(v), Y(0))} width={lb} height={Math.max(1, h)} rx={Math.min(3, lb / 2)} style={{ fill: s.cor, opacity: futuro(i) ? 0.42 : 1 }} /> : null;
              })
            )}
          {tipo === "linha" && n > 1 && (
            <>
              <path d={`M${X(0)},${Y(0)} ${linha.valores.map((v, i) => `L${X(i)},${Y(v)}`).join(" ")} L${X(n - 1)},${Y(0)} Z`} style={{ fill: linha.cor, opacity: 0.14 }} />
              {linha.valores.slice(1).map((v, i) => (
                <line key={i} x1={X(i)} y1={Y(linha.valores[i]!)} x2={X(i + 1)} y2={Y(v)} style={{ stroke: linha.cor, strokeWidth: 3, strokeLinecap: "round", strokeDasharray: futuro(i + 1) ? "5 4" : undefined }} />
              ))}
              {linha.valores.map((v, i) => (
                <circle key={i} cx={X(i)} cy={Y(v)} r={i === atual ? 5 : 3} style={{ fill: v < 0 ? COR.rosa : linha.cor }} />
              ))}
            </>
          )}
          {hover != null && <line x1={X(hover)} x2={X(hover)} y1={m.t} y2={m.t + ph} style={{ stroke: "var(--muted)", strokeWidth: 1, opacity: 0.35 }} />}
          {rotulos.map((r, i) => (
            <rect key={r} x={m.l + passo * i} y={m.t} width={passo} height={ph} fill="transparent" onMouseEnter={() => setHover(i)} onClick={() => setHover(i)} />
          ))}
        </svg>
      )}
      {hover != null && (
        <div className="ch-tip" style={{ top: 0, ...(X(hover) > w / 2 ? { right: w - X(hover) + 10 } : { left: X(hover) + 10 }) }}>
          <b>{rotulos[hover]}</b>
          {series.map((s) => (
            <div key={s.rotulo}>
              <i style={{ display: "inline-block", width: 8, height: 8, borderRadius: 8, background: s.cor, marginRight: 6 }} />
              {s.rotulo}: <b className={s.valores[hover]! < 0 ? "neg" : undefined}>{fmt(s.valores[hover]!)}</b>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/** Barras horizontais com o valor ao lado (funil por etapa). */
export function GraficoBarrasH({ titulo, itens }: { titulo: string; itens: { rotulo: string; valor: number; cor: string }[] }) {
  const max = Math.max(1, ...itens.map((x) => x.valor));
  return (
    <div role="img" aria-label={`${titulo}: ${itens.map((x) => `${x.rotulo} ${x.valor}`).join(", ")}`} style={{ height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-around", gap: 4 }}>
      {itens.map((x) => (
        <div key={x.rotulo} style={{ display: "grid", gridTemplateColumns: "minmax(96px, 34%) minmax(0, 1fr) 28px", gap: 10, alignItems: "center", fontSize: 13 }}>
          <span style={{ textAlign: "right", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{x.rotulo}</span>
          <span style={{ background: "var(--bg)", borderRadius: 4, height: 16, minWidth: 0 }}>
            <span style={{ display: "block", height: "100%", width: `${(x.valor / max) * 100}%`, minWidth: 4, background: x.cor, borderRadius: 4 }} />
          </span>
          <b className="num">{x.valor}</b>
        </div>
      ))}
    </div>
  );
}

const PALETA = [COR.verde, COR.rosa, COR.dourado, COR.azul, COR.ameixa, COR.neutro];

/** Rosca com legenda de valor e participação (despesas por categoria). */
export function GraficoRosca({ titulo, itens }: { titulo: string; itens: { nome: string; valor: number }[] }) {
  const total = itens.reduce((a, x) => a + x.valor, 0) || 1;
  const R = 70;
  const C = 2 * Math.PI * R;
  const fracs = itens.map((x) => x.valor / total);
  const inicio = fracs.map((_, i) => fracs.slice(0, i).reduce((a, f) => a + f, 0));
  return (
    <div style={{ height: "100%", display: "flex", gap: 16, alignItems: "center", flexWrap: "wrap", justifyContent: "center" }}>
      <svg role="img" aria-label={titulo} viewBox="0 0 200 200" style={{ width: 180, height: 180, flex: "none" }}>
        <g transform="rotate(-90 100 100)">
          {itens.map((x, i) => (
            <circle key={x.nome} cx={100} cy={100} r={R} fill="none" style={{ stroke: PALETA[i % PALETA.length], strokeWidth: 30 }} strokeDasharray={`${Math.max(0, fracs[i]! * C - 2)} ${C}`} strokeDashoffset={-inicio[i]! * C} />
          ))}
        </g>
      </svg>
      <ul style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: 6, fontSize: 13, minWidth: 0, flex: "1 1 180px" }}>
        {itens.map((x, i) => (
          <li key={x.nome} style={{ display: "flex", gap: 8, alignItems: "baseline" }}>
            <i style={{ width: 10, height: 10, borderRadius: 10, background: PALETA[i % PALETA.length], flex: "none" }} />
            <span style={{ flex: 1, minWidth: 0, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{x.nome}</span>
            <span className="num" style={{ whiteSpace: "nowrap" }}>
              <b>{brl(x.valor)}</b> <span className="note">{pct(x.valor / total)}</span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
