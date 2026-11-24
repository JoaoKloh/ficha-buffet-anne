"use client";

import { useState } from "react";
import Link from "next/link";
import { Header } from "@/components/Header";
import { Carregando, Sel, Vazio } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { telaDre } from "@/lib/api/gestao/caixa";
import { UNIDADES_DRE, type DreResponseDTO, type RegimeDre, type UnidadeDre } from "@/lib/models/caixa";
import { UN_OPER, unCurto } from "@/lib/models/dominio";
import { MESES_L, brl, capitalizar, fdate, fmtN, pct, plural } from "@/lib/utils/numero";
import { hoje } from "@/lib/utils/datas";
import { EventoSheet } from "../eventos/EventoSheet";
import { RateioForm } from "../caixa/RateioForm";
import { useToast } from "@/components/ui/Toast";
import { imprimirDre } from "./imprimir";

const PREV_KEY = "anne:dre-previstos";

function lerPrev(): boolean {
  try {
    return typeof window === "undefined" || window.localStorage.getItem(PREV_KEY) !== "0";
  } catch {
    return true;
  }
}

/**
 * DRE por unidade de negócio (ou comparando as unidades), por competência ou por
 * caixa, com o rateio da estrutura compartilhada e o resultado por evento.
 */
export function DreView() {
  const { abrir } = useSheet();
  const { showToast } = useToast();
  const [ano, setAno] = useState(hoje().slice(0, 4));
  const [periodo, setPeriodo] = useState(hoje().slice(5, 7));
  const [unidade, setUnidade] = useState<UnidadeDre>("");
  const [regime, setRegime] = useState<RegimeDre>("competencia");
  const [ratear, setRatear] = useState(true);
  const [prev, setPrev] = useState<boolean>(lerPrev);
  const { dados, erro } = useDados(() => telaDre(ano, periodo, prev, unidade, regime, ratear), [ano, periodo, prev, unidade, regime, ratear]);
  const rateioTxt = dados ? UN_OPER.map((u) => `${unCurto(u)} ${fmtN(Math.round(dados.rateio[u]))}%`).join(" · ") : "";
  const mostraRatear = unidade && unidade !== "compartilhado" && unidade !== "_sem";

  return (
    <div className="app">
      <Header
        sub="Resultado por unidade de negócio, por competência ou por caixa"
        actions={
          dados && (
            <>
              <button type="button" className="btn" onClick={() => abrir("Rateio da estrutura compartilhada", <RateioForm rateio={dados.rateio} />)}>
                Rateio da estrutura
              </button>
              <button
                type="button"
                className="btn"
                title="Página A4 para imprimir ou salvar em PDF pelo navegador"
                onClick={() => {
                  if (imprimirDre(dados) === "arquivo") showToast("Arquivo salvo: abra e imprima");
                }}
              >
                Imprimir / baixar
              </button>
            </>
          )
        }
      />
      <div className="toolbar">
        <select className="sel-inline" aria-label="Ano" value={ano} onChange={(e) => setAno(e.target.value)}>
          {(dados?.anos ?? [ano]).map((a) => (
            <option key={a}>{a}</option>
          ))}
        </select>
        <select className="sel-inline" aria-label="Período" value={periodo} onChange={(e) => setPeriodo(e.target.value)}>
          <option value="ano">Ano inteiro</option>
          {MESES_L.map((m, i) => (
            <option key={m} value={String(i + 1).padStart(2, "0")}>
              {capitalizar(m)}
            </option>
          ))}
        </select>
        <Sel className="sel-inline" ariaLabel="Unidade" value={unidade} onChange={(x) => setUnidade(x as UnidadeDre)} opcoes={UNIDADES_DRE} />
        <Sel
          className="sel-inline"
          ariaLabel="Regime"
          value={regime}
          onChange={(x) => setRegime(x as RegimeDre)}
          opcoes={[["competencia", "Competência"], ["caixa", "Caixa (pagamentos)"]] as const}
        />
      </div>
      <div className="toolbar">
        {regime === "competencia" && (
          <label className="check" style={{ fontSize: 14 }}>
            <input
              type="checkbox"
              checked={prev}
              onChange={(e) => {
                setPrev(e.target.checked);
                try {
                  window.localStorage.setItem(PREV_KEY, e.target.checked ? "1" : "0");
                } catch {
                  // preferência de tela
                }
              }}
            />{" "}
            Incluir o que ainda está a receber e a pagar
          </label>
        )}
        {mostraRatear && dados && (
          <label className="check" style={{ fontSize: 14 }}>
            <input type="checkbox" checked={ratear} onChange={(e) => setRatear(e.target.checked)} /> Ratear a estrutura compartilhada ({rateioTxt}
            {dados.rateioDefinido ? "" : ", padrão"})
          </label>
        )}
      </div>
      {dados && dados.semUnidade > 0 && unidade !== "_sem" && (
        <div className="banner">
          {plural(dados.semUnidade, "lançamento", "lançamentos")} ainda sem unidade: {dados.semUnidade > 1 ? "entram" : "entra"} no consolidado, mas não na DRE de
          nenhuma unidade.{" "}
          <Link className="btn sm" href="/caixa?aba=lanc&unidade=_sem">
            Classificar
          </Link>
        </div>
      )}
      {!dados ? (
        <section className="panel">
          <Carregando erro={erro} />
        </section>
      ) : dados.comparativo ? (
        <Comparativo d={dados} />
      ) : (
        <>
          <section className="panel">
            <div className="panel-h">
              <h3>{dados.titulo}</h3>
              <span className="note">
                {dados.descricaoRegime}
                {dados.unidade && dados.ratear ? " · estrutura rateada" : ""}
              </span>
            </div>
            <div className="tablewrap">
              <table>
                <thead>
                  <tr>
                    <th>Linha</th>
                    <th className="r">Valor</th>
                    <th className="r">% receita</th>
                  </tr>
                </thead>
                <tbody>
                  {dados.linhas.map((l, i) => (
                    <tr key={i} className={l.tipo === "grupo" ? "" : l.tipo}>
                      <td>{l.tipo === "fora" ? `Fora do resultado · ${l.rotulo}` : l.rotulo}</td>
                      <td className={`r${l.valor < 0 && l.tipo !== "cat" ? " neg" : ""}`}>{brl(l.valor)}</td>
                      <td className="r">{pct(l.pct)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
          {dados.categoriasInvestimento.length > 0 && (
            <p className="note warn-t">
              {dados.categoriasInvestimento.join(", ")} {dados.categoriasInvestimento.length > 1 ? "estão" : "está"} em uma linha do resultado e por isso{" "}
              {dados.categoriasInvestimento.length > 1 ? "entram" : "entra"} como despesa. Compra de equipamento e obra é investimento: para tirar do resultado, abra a
              categoria em Caixa › Categorias da DRE e mude a linha para &quot;Investimentos (fora do resultado)&quot;.
            </p>
          )}
          {(!dados.unidade || dados.unidade === "buffet") && (
            <section className="panel">
              <div className="panel-h">
                <h3>Resultado por evento</h3>
                <span className="note">receitas e custos ligados ao evento</span>
              </div>
              {dados.resultadoPorEvento.length ? (
                <div className="tablewrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Evento</th>
                        <th>Data</th>
                        <th className="r">Receita</th>
                        <th className="r">Custos</th>
                        <th className="r">Resultado</th>
                        <th className="r">Margem</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dados.resultadoPorEvento.map((x) => (
                        <tr key={x.id} className="clicavel" onClick={() => abrir(x.nome, <EventoSheet id={x.id} />)}>
                          <td>{x.nome}</td>
                          <td>{fdate(x.dataEvento)}</td>
                          <td className="r">{brl(x.rec)}</td>
                          <td className="r">{brl(x.desp)}</td>
                          <td className={`r${x.res < 0 ? " neg" : ""}`}>{brl(x.res)}</td>
                          <td className="r">{pct(x.mg)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <Vazio titulo="Nenhum evento com lançamentos neste período">Ligue custos e receitas a um evento para ver a margem de cada um.</Vazio>
              )}
            </section>
          )}
        </>
      )}
    </div>
  );
}

/** Uma coluna por unidade, mais o consolidado. */
function Comparativo({ d }: { d: DreResponseDTO }) {
  const c = d.comparativo!;
  return (
    <>
      <section className="panel">
        <div className="panel-h">
          <h3>{d.titulo.replace(" · comparativo das unidades", "")} · por unidade</h3>
          <span className="note">
            {d.descricaoRegime}
            {d.ratear ? " · estrutura rateada" : ""}
          </span>
        </div>
        <div className="tablewrap">
          <table>
            <thead>
              <tr>
                <th>Linha</th>
                {c.colunas.map((col) => (
                  <th key={col.chave} className="r">
                    {col.rotulo}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {c.linhas.map((l) => (
                <tr key={l.rotulo} className={l.tipo === "grupo" ? "" : l.tipo}>
                  <td>{l.rotulo}</td>
                  {l.valores.map((v, i) => (
                    <td key={i} className={`r${v < 0 ? " neg" : ""}`}>
                      {brl(v)}
                    </td>
                  ))}
                </tr>
              ))}
              <tr>
                <td>Margem líquida</td>
                {c.margens.map((m, i) => (
                  <td key={i} className="r">
                    {pct(m)}
                  </td>
                ))}
              </tr>
              {c.investimentos && (
                <tr className="cat">
                  <td>Investimentos no período (fora do resultado)</td>
                  {c.investimentos.map((v, i) => (
                    <td key={i} className="r">
                      {brl(v)}
                    </td>
                  ))}
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
      <p className="note">
        Cada unidade mostra o próprio resultado, mesmo quando divide estrutura, funcionários ou estoque.{" "}
        {d.ratear ? "A estrutura compartilhada é distribuída pelo percentual do rateio. " : "A estrutura compartilhada aparece em coluna própria. "}
        Transferências tiram custo de uma unidade e põem em outra, sem mudar o consolidado.
      </p>
    </>
  );
}
