"use client";

import { useState } from "react";
import { Header } from "@/components/Header";
import { Carregando, Chips, Kpi, Pill, Vazio } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { CATEGORIAS_PREPARACAO, META_CMV, SECOES_CARTA } from "@/lib/models/dominio";
import { listarPreparacoes, telaPratosVenda } from "@/lib/api/gestao/fichas";
import { brl, fmtN, pct } from "@/lib/utils/numero";
import { FichaForm } from "./FichaForm";
import { PratoVendaForm } from "./PratoVendaForm";

const ABAS = [
  ["preparacoes", "Preparações"],
  ["restaurante", "Pratos do restaurante"],
] as const;
type Aba = (typeof ABAS)[number][0];
const unidRend = (u: string, n: number) => (u === "porções" ? (n === 1 ? "porção" : "porções") : u === "un" ? "un" : u);
const tomCmv = (r: number | null) => (r == null ? "" : r <= META_CMV ? "good" : r <= META_CMV + 0.06 ? "warn" : "bad");

export function FichasView() {
  const [aba, setAba] = useState<Aba>("preparacoes");
  return aba === "preparacoes" ? <Preparacoes aba={aba} setAba={setAba} /> : <Restaurante aba={aba} setAba={setAba} />;
}

function Preparacoes({ aba, setAba }: { aba: Aba; setAba: (a: Aba) => void }) {
  const { abrir } = useSheet();
  const [busca, setBusca] = useState("");
  const { dados, erro } = useDados(() => listarPreparacoes(busca), [busca]);
  const grupos = [...CATEGORIAS_PREPARACAO, ["__", "Outros"] as const];
  return (
    <div className="app">
      <Header
        titulo="Fichas técnicas"
        sub={dados ? `${dados.length} preparações · itens em Estoque` : undefined}
        actions={
          <button type="button" className="btn primary" onClick={() => abrir("Nova ficha técnica", <FichaForm />)}>
            Nova ficha
          </button>
        }
      />
      <Chips opcoes={ABAS} valor={aba} onChange={setAba} rotulo="Tipo de ficha" />
      <div className="toolbar">
        <input className="search" type="search" placeholder="Buscar preparação" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar preparação" />
      </div>
      <section className="panel">
        <div className="panel-b">
          {!dados ? (
            <Carregando erro={erro} />
          ) : !dados.length ? (
            <Vazio titulo={busca ? "Nenhuma ficha encontrada" : "Nenhuma ficha técnica"}>Cada ficha diz quanto a receita rende e quais insumos leva. O sistema usa isso para calcular custo, compras e produção.</Vazio>
          ) : (
            grupos.map(([k, l]) => {
              const ps = dados.filter((p) => (k === "__" ? !CATEGORIAS_PREPARACAO.some((c) => c[0] === p.categoria) : p.categoria === k));
              if (!ps.length) return null;
              return (
                <div key={k}>
                  <div className="monthhead">{l}</div>
                  {ps.map((p) => {
                    const u = p.unidadeRendimento || "porções";
                    const porc = Number(p.porcaoPadrao) || 0;
                    return (
                      <button key={p.id} type="button" className="row" onClick={() => abrir(p.nome, <FichaForm ficha={p} />)}>
                        <span className="grow">
                          <span className="t">{p.nome}</span>
                          <span className="m">
                            rende {fmtN(p.rendimento)} {unidRend(u, p.rendimento)} · {p.itens.length} insumo{p.itens.length === 1 ? "" : "s"}
                            {p.etapas.length ? ` · ${p.etapas.length} etapa${p.etapas.length > 1 ? "s" : ""}` : ""}
                          </span>
                        </span>
                        <Pill tom={p.fichaStatus === "Completa" ? "good" : p.fichaStatus === "Sem preço" ? "warn" : "bad"}>{p.fichaStatus}</Pill>
                        <span className="amt">
                          {p.custo.total ? (
                            <>
                              {brl(p.custo.porUnid * porc)}
                              <span className="m" style={{ display: "inline" }}>
                                {" "}
                                /conv.
                              </span>
                            </>
                          ) : (
                            "—"
                          )}
                        </span>
                      </button>
                    );
                  })}
                </div>
              );
            })
          )}
        </div>
      </section>
    </div>
  );
}

function Restaurante({ aba, setAba }: { aba: Aba; setAba: (a: Aba) => void }) {
  const { abrir } = useSheet();
  const [busca, setBusca] = useState("");
  const { dados, erro } = useDados(() => telaPratosVenda(busca), [busca]);
  const k = dados?.kpis;
  return (
    <div className="app">
      <Header
        titulo="Fichas técnicas"
        sub={dados ? `${dados.pratos.length} pratos do Restaurante ANNE · montados com as fichas` : undefined}
        actions={
          <button type="button" className="btn primary" onClick={() => abrir("Novo prato do restaurante", <PratoVendaForm />)}>
            Novo prato
          </button>
        }
      />
      <Chips opcoes={ABAS} valor={aba} onChange={setAba} rotulo="Tipo de ficha" />
      {k && (
        <div className="kpis">
          <Kpi k="Pratos ativos" v={String(k.ativos)} h={k.foraDaCarta ? `${k.foraDaCarta} fora da carta` : "na carta"} />
          <Kpi k="CMV médio" v={k.cmvMedio != null ? pct(k.cmvMedio) : "—"} negativo={tomCmv(k.cmvMedio) === "bad"} h={`meta ${pct(META_CMV)}`} />
          <Kpi k="Acima da meta" v={String(k.acimaDaMeta)} negativo={k.acimaDaMeta > 0} h={k.acimaDaMeta ? "reveja preço ou porção" : "nenhum"} />
          <Kpi k="Incompletos" v={String(k.incompletos)} h={k.incompletos ? "sem ficha, preço ou custo" : "tudo calculado"} />
        </div>
      )}
      <div className="toolbar">
        <input className="search" type="search" placeholder="Buscar prato" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar prato" />
      </div>
      <section className="panel">
        <div className="panel-b">
          {!dados ? (
            <Carregando erro={erro} />
          ) : !dados.pratos.length ? (
            <Vazio titulo={busca ? "Nenhum prato encontrado" : "Nenhum prato do restaurante cadastrado"}>Cada prato da carta é montado com as fichas técnicas que leva e a quantidade por prato. O sistema calcula o custo e o CMV pelo preço de venda.</Vazio>
          ) : (
            SECOES_CARTA.map(([s, l]) => {
              const ps = dados.pratos.filter((x) => (x.secao || "outro") === s);
              if (!ps.length) return null;
              return (
                <div key={s}>
                  <div className="monthhead">{l}</div>
                  {ps.map((x) => {
                    const c = x.cmv;
                    const tom = tomCmv(c.cmv);
                    return (
                      <button key={x.id} type="button" className="row" style={x.ativo === false ? { opacity: 0.6 } : undefined} onClick={() => abrir(x.nome, <PratoVendaForm prato={x} />)}>
                        <span className="grow">
                          <span className="t">
                            {x.nome}
                            {x.ativo === false ? " · fora da carta" : ""}
                          </span>
                          <span className="m">
                            {c.cmv != null && !c.faltando ? <b style={{ color: `var(--${tom === "good" ? "good" : tom === "warn" ? "warn" : "bad"})` }}>CMV {pct(c.cmv)}</b> : <b style={{ color: "var(--warn)" }}>{x.preco > 0 ? "custo incompleto" : "sem preço"}</b>} ·{" "}
                            {x.componentesNomes.length ? x.componentesNomes.join(" + ") : "sem fichas ligadas"}
                            {x.codigoPdv ? ` · PDV ${x.codigoPdv}` : ""}
                          </span>
                        </span>
                        <span className="amt">
                          {x.preco > 0 ? brl(x.preco) : "—"}
                          <span className="m">custo {brl(c.custo)}</span>
                        </span>
                      </button>
                    );
                  })}
                </div>
              );
            })
          )}
        </div>
      </section>
      <p className="note">
        CMV = custo dos ingredientes do prato ÷ preço de venda. Verde até {pct(META_CMV)}, amarelo até {pct(META_CMV + 0.06)}, vermelho acima. O custo vem das fichas técnicas e do preço dos insumos no cadastro.
      </p>
    </div>
  );
}
