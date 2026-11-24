"use client";

import { useEffect, useRef, useState } from "react";
import { Carregando, Confirma, Fld, Pill, Sel, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { FORMAS_PAGAMENTO_NF, UNIDADES, rotulo, type FormaPagamentoNf, type Unidade } from "@/lib/models/dominio";
import type { NotaFiscalImportadaDTO, NotaFiscalListaItemDTO, SalvarNotaFiscalRequestDTO } from "@/lib/models/estoque";
import { deParaNf, estornarNf, excluirNf, importarXmlNf, lancarNf, lerNfPorFoto, obterNf, opcoesEstoque, salvarRascunhoNf } from "@/lib/api/gestao/estoque";
import { marcarPago } from "@/lib/api/gestao/caixa";
import { brl, dec, fdate, fmtN, numIn } from "@/lib/utils/numero";
import { addDays, addMonths, hoje, round2 } from "@/lib/utils/datas";
import { conferirLinha, gerarParcelas, qtdConvertida, somaItens, somaParcelas, totalNf } from "@/lib/utils/nf";
import { InsumoForm } from "../fichas/InsumoForm";

interface LinhaD { desc: string; cod: string; uCom: string; insumoId: string; qtdNota: string; unid: "estoque" | "compra"; valorUnit: number | null; valor: string; conferido: boolean }
interface ParcD { venc: string; valor: string; pago: boolean }
const linhaVazia = (): LinhaD => ({ desc: "", cod: "", uCom: "", insumoId: "", qtdNota: "", unid: "estoque", valorUnit: null, valor: "", conferido: false });
const INTERVALOS = [["7", "7 dias"], ["14", "14 dias"], ["15", "15 dias"], ["21", "21 dias"], ["28", "28 dias"], ["30", "30 dias"], ["m", "Mensal (mesmo dia)"]] as const;

/** Nota fiscal de entrada: destino, de-para dos itens, conferência, pagamento e entrada no estoque. */
export function NfSheet({ nf, pre }: { nf?: NotaFiscalListaItemDTO; pre?: NotaFiscalImportadaDTO }) {
  const { dados: op, erro } = useDados(() => opcoesEstoque("nf"));
  const { fechar, recarregar, abrir } = useSheet();
  const { showToast } = useToast();
  const lancada = nf?.status === "lancada";
  const src = nf ?? pre;
  const [d, setD] = useState({
    fornecedor: src?.fornecedor ?? "",
    numero: src?.numero ?? "",
    emissao: src?.emissao ?? hoje(),
    eventoId: nf?.eventoId ? String(nf.eventoId) : "",
    unidade: (nf?.unidade ?? (nf?.eventoId ? "buffet" : "")) as Unidade | "",
    lancarCaixa: nf ? nf.lancarCaixa : true,
    categoriaId: nf?.categoriaId ? String(nf.categoriaId) : "",
    formaPag: (src?.formaPag ?? (nf ? (nf.pago ? "pix" : "boleto") : "pix")) as FormaPagamentoNf,
    acrescimo: dec(src?.acrescimo || ""),
    nParc: "",
    intervalo: "30",
  });
  const [itens, setItens] = useState<LinhaD[]>(() =>
    src?.itens.length ? src.itens.map((it) => ({ desc: it.desc, cod: it.cod, uCom: it.uCom, insumoId: it.insumoId ? String(it.insumoId) : "", qtdNota: dec(it.qtdNota), unid: it.unid, valorUnit: it.valorUnit, valor: dec(it.valor), conferido: it.conferido })) : [linhaVazia()]
  );
  const [parcelas, setParcelas] = useState<ParcD[]>(() =>
    src?.parcelas.length ? src.parcelas.map((p) => ({ venc: p.venc, valor: dec(p.valor), pago: p.pago })) : [{ venc: src?.emissao ?? hoje(), valor: nf?.total ? dec(nf.total) : "", pago: nf ? nf.pago : true }]
  );
  const [erroForm, setErroForm] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState<string | null>(null);
  const [conf, setConf] = useState<{ msg: string; fn: () => void } | null>(null);
  if (!op) return <Carregando erro={erro} />;
  const categoria = d.categoriaId || (op.categoriaAB ? String(op.categoriaAB) : "");

  const info = (id: string) => (id ? op.itensInfo[Number(id)] : undefined);
  const qtdLinha = (it: LinhaD) => (info(it.insumoId) ? qtdConvertida(numIn(it.qtdNota), it.unid, info(it.insumoId)!.fatorCompra) : 0);
  const vals = itens.map((it) => ({ valor: numIn(it.valor) }));
  const total = totalNf(vals, numIn(d.acrescimo));
  const sParc = somaParcelas(parcelas.map((p) => ({ valor: numIn(p.valor) })));
  const updItem = (ix: number, m: Partial<LinhaD>) => setItens((l) => l.map((x, i) => (i === ix ? { ...x, ...m } : x)));
  const updParc = (ix: number, m: Partial<ParcD>) => setParcelas((l) => l.map((x, i) => (i === ix ? { ...x, ...m } : x)));

  async function aplicarDePara(fornecedor: string, linhas: LinhaD[], soVazias = true) {
    if (!fornecedor.trim()) return;
    try {
      const sug = await deParaNf({ fornecedor, itens: linhas.map((it) => ({ desc: it.desc, cod: it.cod })) });
      let hit = false;
      setItens((l) =>
        l.map((it, i) => {
          const s = sug[i];
          if (s && (!soVazias || !it.insumoId) && (it.desc || it.cod)) {
            hit = true;
            return { ...it, insumoId: String(s.insumoId), unid: s.unid };
          }
          return it;
        })
      );
      if (hit) showToast("Itens ligados pelo de-para deste fornecedor");
    } catch {
      // sugestão é opcional
    }
  }

  const dto = (): SalvarNotaFiscalRequestDTO => ({
    fornecedor: d.fornecedor.trim(), numero: d.numero.trim(), emissao: d.emissao, eventoId: d.eventoId ? Number(d.eventoId) : null, lancarCaixa: d.lancarCaixa,
    categoriaId: categoria ? Number(categoria) : null, formaPag: d.formaPag, parcelas: parcelas.map((p) => ({ venc: p.venc || d.emissao, valor: numIn(p.valor), pago: p.pago })),
    itens: itens.map((it) => ({ desc: it.desc, cod: it.cod, uCom: it.uCom, insumoId: it.insumoId ? Number(it.insumoId) : null, qtdNota: numIn(it.qtdNota), unid: it.unid, valorUnit: it.valorUnit, valor: numIn(it.valor), conferido: it.conferido })),
    acrescimo: numIn(d.acrescimo), chave: src?.chave ?? "", serie: src?.serie ?? "", razaoSocial: src?.razaoSocial ?? "", cnpjFornecedor: src?.cnpjFornecedor ?? "",
    importadaXML: !!src?.importadaXML, importadaFoto: !!src?.importadaFoto, unidade: d.unidade, aceitarDiferenca: false,
  });

  async function executar(chave: string, fn: () => Promise<unknown>, ok: string) {
    setOcupado(chave);
    setErroForm(null);
    try {
      await fn();
      showToast(ok);
      recarregar();
      fechar();
    } catch (e) {
      setErroForm(msgErro(e));
    } finally {
      setOcupado(null);
    }
  }

  function dividir() {
    const n = Math.round(Number(d.nParc || parcelas.length) || 0);
    if (!(n >= 1 && n <= 48)) return setErroForm("Informe de 1 a 48 parcelas.");
    if (!(total > 0)) return setErroForm("Preencha os valores dos itens antes de dividir.");
    setErroForm(null);
    const pg = d.formaPag !== "boleto" && d.formaPag !== "prazo" && n === 1 && !!parcelas[0]?.pago;
    setParcelas(gerarParcelas(total, n, parcelas[0]?.venc || d.emissao, d.intervalo, pg).map((p) => ({ venc: p.venc, valor: dec(p.valor), pago: p.pago })));
  }

  function darEntrada() {
    const x = dto();
    if (!x.fornecedor) return setErroForm("Informe o fornecedor.");
    if (!x.emissao) return setErroForm("Informe a data de emissão.");
    const ok = `Entrada lançada no estoque${x.lancarCaixa && total > 0 ? ` e ${x.parcelas.length} parcela${x.parcelas.length > 1 ? "s" : ""} no caixa` : ""}`;
    if (x.lancarCaixa && total > 0) {
      if (!x.unidade) return setErroForm("Escolha a unidade de negócio.");
      // Parcelas diferentes do total dos produtos: entra assim mesmo, com a diferença registrada na nota.
      const dif = round2(sParc - total);
      if (Math.abs(dif) >= 0.01) {
        setErroForm(null);
        return setConf({
          msg: `A soma das parcelas (${brl(sParc)}) é ${dif < 0 ? "menor" : "maior"} que o total dos produtos (${brl(total)}), diferença de ${brl(Math.abs(dif))}. Dar entrada assim mesmo? O estoque entra com os valores dos produtos e o caixa com o valor das parcelas; a diferença fica registrada na nota como ${dif < 0 ? "desconto" : "acréscimo"}.`,
          fn: () => executar("go", () => lancarNf(nf?.id ?? null, { ...x, aceitarDiferenca: true }), ok),
        });
      }
    }
    executar("go", () => lancarNf(nf?.id ?? null, x), ok);
  }

  const nConf = itens.filter((i) => i.conferido).length;
  const dis = lancada;
  return (
    <>
      {lancada && <div className="banner">Nota lançada em {fdate(nf!.lancadaEm)}. Para corrigir itens, destino ou parcelas, estorne a entrada: os movimentos e as contas a pagar saem e a nota volta a rascunho.</div>}
      {pre?.importadaFoto && (
        <div className="banner">
          <b>Lido da foto.</b> Confira cada linha com a nota em mãos (descrição, quantidade, valor) e os vencimentos antes de dar entrada.
          {pre.duvidas.length > 0 && (
            <>
              <br />
              Pontos a conferir: {pre.duvidas.join("; ")}.
            </>
          )}
        </div>
      )}
      {pre && (
        <div className="summary">
          <span>
            <b>{pre.importadaFoto ? "Lido" : "XML lido"}:</b> {pre.razaoSocial || pre.fornecedor}
            {pre.cnpjFornecedor ? ` · CNPJ ${pre.cnpjFornecedor}` : ""} · {pre.itens.length} ite{pre.itens.length === 1 ? "m" : "ns"} · {brl(pre.total)}
            {pre.parcelas.length > 1 || pre.formaPag === "boleto" ? ` · ${pre.parcelas.length} parcela${pre.parcelas.length > 1 ? "s" : ""} no boleto` : ""}
          </span>
          <span>Escolha o destino, ligue cada linha ao cadastro (o sistema lembra nas próximas notas deste fornecedor) e confira.</span>
        </div>
      )}
      <div className="form">
        <Fld label="Fornecedor" obrig>
          <input list="nf_forn_list" value={d.fornecedor} disabled={dis} onChange={(e) => setD({ ...d, fornecedor: e.target.value })} onBlur={() => aplicarDePara(d.fornecedor, itens)} />
          <datalist id="nf_forn_list">
            {op.fornecedores.map((f) => (
              <option key={f} value={f} />
            ))}
          </datalist>
        </Fld>
        <Fld label="Número da NF">
          <input inputMode="numeric" value={d.numero} disabled={dis} onChange={(e) => setD({ ...d, numero: e.target.value })} />
        </Fld>
        <Fld label="Emissão" obrig>
          <input
            type="date"
            value={d.emissao}
            disabled={dis}
            onChange={(e) => {
              const v = e.target.value;
              setD({ ...d, emissao: v });
              if (parcelas.length === 1 && !parcelas[0]!.pago && d.formaPag !== "boleto") updParc(0, { venc: v });
            }}
          />
        </Fld>
        <Fld label="Destino da compra" obrig hint="Escolha o evento quando a compra for para ele: o custo entra no resultado do evento.">
          <Sel
            value={d.eventoId}
            disabled={dis}
            onChange={(x) => setD({ ...d, eventoId: x, unidade: x ? "buffet" : d.unidade })}
            opcoes={op.eventosAtivos.map(([id, n]) => [String(id), n] as const)}
            vazio="Estoque da fábrica (uso geral)"
          />
        </Fld>
        <Fld label="Unidade de negócio" obrig hint="Quem paga e usa a compra. Compra para as três vai em Estrutura compartilhada.">
          <Sel value={d.unidade} disabled={dis} onChange={(x) => setD({ ...d, unidade: x as Unidade })} opcoes={UNIDADES} vazio="Escolha…" />
        </Fld>
      </div>

      <div className="sec">
        <div className="sec-h">
          <h3>Itens da nota</h3>
          <span className="note">
            {nConf} de {itens.length} conferidos
          </span>
        </div>
        {itens.map((it, ix) => {
          const i = info(it.insumoId);
          const cq = qtdLinha(it);
          const cl = conferirLinha(numIn(it.qtdNota), it.valorUnit, numIn(it.valor));
          return (
            <div key={ix} className={`envcard${it.conferido ? " ready" : ""}`}>
              <div className="envgrid">
                <Fld label={`Item na nota${it.cod ? ` · cód. ${it.cod}` : ""}${it.uCom ? ` · ${it.uCom}` : ""}`} full>
                  <input value={it.desc} disabled={dis} placeholder="Ex.: RUCULA HIDROP MACO" onChange={(e) => updItem(ix, { desc: e.target.value })} onBlur={() => !it.insumoId && aplicarDePara(d.fornecedor, itens)} />
                </Fld>
                <div className="fld wide">
                  <span>Item no cadastro (de-para)</span>
                  <Sel
                    value={it.insumoId}
                    disabled={dis}
                    onChange={(x) => {
                      if (x === "__novo")
                        return abrir("Novo item", <InsumoForm preset={{ nome: it.desc ? it.desc.charAt(0) + it.desc.slice(1).toLowerCase() : "", fornecedor: d.fornecedor }} onSalvo={(n) => updItem(ix, { insumoId: String(n.id) })} />);
                      updItem(ix, { insumoId: x, unid: "estoque" });
                    }}
                    opcoes={[["__novo", "+ Novo item no cadastro"] as const]}
                    grupos={op.itens.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))}
                    vazio="Escolha…"
                  />
                </div>
                <Fld label="Qtd na nota">
                  <input inputMode="decimal" value={it.qtdNota} disabled={dis} onChange={(e) => updItem(ix, { qtdNota: e.target.value })} />
                </Fld>
                <Fld label="Unidade da nota">
                  <Sel
                    value={it.unid}
                    disabled={dis}
                    onChange={(x) => updItem(ix, { unid: x as "estoque" | "compra" })}
                    opcoes={[["estoque", i ? i.unidade : "unid. de estoque"] as const, ...(i && Number(i.fatorCompra) > 0 ? [["compra", `${i.unidadeCompra || "embalagem"} (${fmtN(i.fatorCompra)} ${i.unidade})`] as const] : [])]}
                  />
                </Fld>
                <Fld label="Valor da linha (R$)">
                  <input inputMode="decimal" value={it.valor} disabled={dis} onChange={(e) => updItem(ix, { valor: e.target.value })} />
                </Fld>
              </div>
              <div className="toolbar between">
                <span className="note">
                  {cl && `${cl.ok ? "Na nota: " : "⚠ Na nota: "}${fmtN(numIn(it.qtdNota))} × ${brl(it.valorUnit)} = ${brl(cl.esperado)}${cl.ok ? " ✓" : " — diferente do V. total digitado"} · `}
                  {i && cq ? `Entra ${fmtN(cq)} ${i.unidade}${numIn(it.valor) ? ` · ${brl(numIn(it.valor) / cq)} /${i.unidade}` : ""}` : "Ligue ao cadastro e informe a quantidade."}
                </span>
                <span className="toolbar">
                  <label className="check" style={{ fontSize: 14 }}>
                    <input type="checkbox" checked={it.conferido} disabled={dis} onChange={(e) => updItem(ix, { conferido: e.target.checked })} /> Conferido
                  </label>
                  {!lancada && (
                    <button type="button" className="btn sm ghost" aria-label="Remover linha" onClick={() => setItens((l) => (l.length > 1 ? l.filter((_, i2) => i2 !== ix) : [linhaVazia()]))}>
                      ✕
                    </button>
                  )}
                </span>
              </div>
            </div>
          );
        })}
        {!lancada && (
          <div>
            <button type="button" className="btn sm" onClick={() => setItens((l) => [...l, linhaVazia()])}>
              Adicionar linha
            </button>
          </div>
        )}
      </div>

      <div className="form">
        <Fld label="Impostos, frete e descontos cobrados à parte (R$)" full hint="Diferença entre o que as duplicatas/boleto cobram e a soma dos V. TOTAL dos produtos (ex.: ICMS ST, frete; desconto com sinal de menos). Vai só para o caixa: não muda o valor nem o custo de nenhum produto.">
          <input inputMode="decimal" placeholder="0,00" value={d.acrescimo} disabled={dis} onChange={(e) => setD({ ...d, acrescimo: e.target.value })} />
        </Fld>
      </div>
      <div className="summary">
        <span>
          Soma dos produtos (V. TOTAL): <b className="num">{brl(somaItens(vals))}</b>
        </span>
        <span>
          Total da nota: <b className="num">{brl(total)}</b>
        </span>
      </div>

      <div className="sec">
        <div className="sec-h">
          <h3>Pagamento</h3>
        </div>
        {lancada ? (
          <div className="box">
            {nf!.lancamentos.length ? (
              nf!.lancamentos.map((l, ix) => {
                const v = l.vencimento || l.dataCompetencia;
                const venc = l.status === "previsto" && v < hoje();
                return (
                  <div key={l.id} className="row static">
                    <span className="grow">
                      <span className="t">
                        Parcela {l.parcelaNF ?? ix + 1}/{nf!.lancamentos.length}
                        {l.formaPag ? ` · ${rotulo(FORMAS_PAGAMENTO_NF, l.formaPag)}` : ""}
                      </span>
                      <span className="m" style={venc ? { color: "var(--bad)" } : undefined}>
                        {l.status === "realizado" ? `paga em ${fdate(l.dataPagamento)}` : `${venc ? "venceu em " : "vence em "}${fdate(v)}`}
                      </span>
                    </span>
                    {l.status === "previsto" ? (
                      <button
                        type="button"
                        className="btn sm"
                        onClick={async () => {
                          try {
                            await marcarPago(l.id);
                            showToast("Parcela paga hoje");
                            recarregar();
                            fechar();
                            abrir(`NF ${nf!.numero || "s/n"}`, <NfSheet nf={await obterNf(nf!.id)} />);
                          } catch (e) {
                            showToast(msgErro(e), "error");
                          }
                        }}
                      >
                        Pagar
                      </button>
                    ) : (
                      <Pill tom="good">paga</Pill>
                    )}
                    <span className="amt">{brl(l.valor)}</span>
                  </div>
                );
              })
            ) : (
              <div className="empty">Esta nota não foi lançada no caixa.</div>
            )}
          </div>
        ) : (
          <>
            <div className="form">
              <label className="check" style={{ gridColumn: "1/-1" }}>
                <input type="checkbox" checked={d.lancarCaixa} onChange={(e) => setD({ ...d, lancarCaixa: e.target.checked })} /> Lançar as parcelas em contas a pagar (caixa)
              </label>
              <Fld label="Forma de pagamento">
                <Sel
                  value={d.formaPag}
                  onChange={(x) => {
                    setD({ ...d, formaPag: x as FormaPagamentoNf });
                    if (x === "boleto" || x === "prazo") setParcelas((l) => l.map((p) => ({ ...p, pago: false })));
                  }}
                  opcoes={FORMAS_PAGAMENTO_NF}
                />
              </Fld>
              <Fld label="Categoria da despesa">
                <Sel value={categoria} onChange={(x) => setD({ ...d, categoriaId: x })} grupos={op.categoriasDespesa.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))} vazio="Escolha…" />
              </Fld>
            </div>
            <div className="toolbar" style={{ alignItems: "flex-end" }}>
              <Fld label="Parcelas">
                <input inputMode="numeric" style={{ width: 110 }} value={d.nParc || String(parcelas.length)} onChange={(e) => setD({ ...d, nParc: e.target.value })} />
              </Fld>
              <Fld label="Intervalo">
                <Sel value={d.intervalo} onChange={(x) => setD({ ...d, intervalo: x })} opcoes={INTERVALOS} />
              </Fld>
              <button type="button" className="btn sm" onClick={dividir}>
                Dividir o total
              </button>
            </div>
            <p className="note">Defina o 1º vencimento na primeira linha e toque em Dividir o total. Depois ajuste datas e valores exatamente como estão no boleto.</p>
            <div className="fitems">
              {parcelas.map((p, ix) => (
                <div key={ix} className="fitem parc">
                  <span className="note num" style={{ paddingBottom: 10 }}>
                    {ix + 1}
                  </span>
                  <Fld label="Vencimento">
                    <input type="date" value={p.venc} onChange={(e) => updParc(ix, { venc: e.target.value })} />
                  </Fld>
                  <Fld label="Valor (R$)">
                    <input inputMode="decimal" value={p.valor} onChange={(e) => updParc(ix, { valor: e.target.value })} />
                  </Fld>
                  <label className="check" style={{ fontSize: 14, paddingBottom: 10 }}>
                    <input type="checkbox" checked={p.pago} onChange={(e) => updParc(ix, { pago: e.target.checked })} /> Paga
                  </label>
                  <button type="button" className="btn sm ghost" aria-label="Remover parcela" disabled={parcelas.length < 2} onClick={() => setParcelas((l) => l.filter((_, i) => i !== ix))}>
                    ✕
                  </button>
                </div>
              ))}
            </div>
            <div>
              <button
                type="button"
                className="btn sm"
                onClick={() => {
                  const last = parcelas[parcelas.length - 1];
                  setParcelas((l) => [...l, { venc: last ? (d.intervalo === "m" ? addMonths(last.venc, 1) : addDays(last.venc, Number(d.intervalo) || 30)) : d.emissao, valor: "", pago: false }]);
                }}
              >
                Adicionar parcela
              </button>
            </div>
            <p className="note">
              Soma das parcelas: <b className="num">{brl(sParc)}</b>
              {Math.abs(total - sParc) < 0.01 ? " · confere com o total" : <span style={{ color: "var(--warn)" }}> · diferença de {brl(Math.abs(total - sParc))} para o total da nota</span>}
            </p>
          </>
        )}
      </div>

      {erroForm && <p className="erro-form" role="alert">{erroForm}</p>}
      <div className="actions">
        {lancada ? (
          <>
            <button
              type="button"
              className="btn danger left"
              onClick={() => {
                const pg = nf!.lancamentos.filter((l) => l.status === "realizado").length;
                setConf({
                  msg: `Estornar a entrada? Os movimentos de estoque e as ${nf!.lancamentos.length} parcela(s) desta nota no caixa serão excluídos${pg ? `, inclusive ${pg} já paga(s) — relance o pagamento depois` : ""}. O preço dos itens fica como está.`,
                  fn: () => executar("estorno", () => estornarNf(nf!.id), "Entrada estornada"),
                });
              }}
            >
              Estornar entrada
            </button>
            <button type="button" className="btn" onClick={() => fechar()}>
              Fechar
            </button>
          </>
        ) : (
          <>
            {nf && (
              <button type="button" className="btn danger left" onClick={() => setConf({ msg: "Excluir este rascunho de nota?", fn: () => executar("del", () => excluirNf(nf.id), "Rascunho excluído") })}>
                Excluir rascunho
              </button>
            )}
            <button type="button" className="btn" disabled={!!ocupado} onClick={() => (d.fornecedor.trim() ? executar("save", () => salvarRascunhoNf(nf?.id ?? null, dto()), "Rascunho salvo") : setErroForm("Informe o fornecedor."))}>
              Salvar rascunho
            </button>
            <button type="button" className="btn primary" disabled={!!ocupado} onClick={darEntrada}>
              {ocupado === "go" ? "Lançando…" : "Dar entrada"}
            </button>
          </>
        )}
      </div>
      {conf && (
        <Confirma
          msg={conf.msg}
          onNao={() => setConf(null)}
          onSim={() => {
            conf.fn();
            setConf(null);
          }}
        />
      )}
    </>
  );
}

/** Botões "Importar XML da NF-e" e "Ler nota pela foto" (a leitura por foto ainda não processa). */
export function useImportarNf() {
  const { abrir } = useSheet();
  const { showToast } = useToast();
  const xmlRef = useRef<HTMLInputElement | null>(null);
  const fotoRef = useRef<HTMLInputElement | null>(null);

  async function aoEscolherXml(f: File | undefined) {
    if (!f) return;
    if (f.size > 5 * 1024 * 1024) return showToast("Arquivo grande demais para um XML de NF-e.", "error");
    try {
      const x = await importarXmlNf(await f.text());
      if (x.duplicadaDe) {
        const dup = await obterNf(x.duplicadaDe);
        showToast(`Esta nota já foi importada (NF ${dup.numero || "s/n"}).`, "error");
        return abrir(`NF ${dup.numero || "s/n"}`, <NfSheet nf={dup} />, { larga: true });
      }
      abrir(`Importar NF ${x.numero}`, <NfSheet pre={x} />, { larga: true });
    } catch (e) {
      showToast(msgErro(e, "Não consegui ler o XML."), "error");
    }
  }

  function aoEscolherFotos(fs: File[]) {
    if (!fs.length) return;
    if (fs.length > 5) return showToast("Envie até 5 fotos da mesma nota.", "error");
    if (fs.some((f) => !/^image\/(jpeg|png|webp)$/.test(f.type))) return showToast("Use fotos em JPG, PNG ou WebP.", "error");
    if (fs.some((f) => f.size > 10 * 1024 * 1024)) return showToast("Uma das fotos é grande demais. Tire outra ou reduza.", "error");
    abrir("Ler nota pela foto", <FotoSheet n={fs.length} onOutra={() => fotoRef.current?.click()} />);
  }

  const inputs = (
    <>
      <input ref={xmlRef} type="file" accept=".xml,text/xml,application/xml" hidden onChange={(e) => (aoEscolherXml(e.target.files?.[0]), (e.target.value = ""))} />
      <input ref={fotoRef} type="file" accept="image/jpeg,image/png,image/webp" multiple hidden onChange={(e) => (aoEscolherFotos([...(e.target.files ?? [])]), (e.target.value = ""))} />
    </>
  );
  return { inputs, escolherXml: () => xmlRef.current?.click(), escolherFoto: () => fotoRef.current?.click() };
}

/** Leitura por foto: a interface do protótipo; o servidor ainda responde que o recurso não está disponível. */
function FotoSheet({ n, onOutra }: { n: number; onOutra: () => void }) {
  const { fechar } = useSheet();
  const [estado, setEstado] = useState<{ st: "lendo" | "erro"; erro: string }>({ st: "lendo", erro: "" });
  useEffect(() => {
    let vivo = true;
    lerNfPorFoto().then(
      () => vivo && setEstado({ st: "erro", erro: "Não consegui entender a nota nesta foto." }),
      (e: unknown) => vivo && setEstado({ st: "erro", erro: msgErro(e, "Falha de conexão ao ler a nota. Tente de novo.") })
    );
    return () => {
      vivo = false;
    };
  }, []);
  if (estado.st === "lendo")
    return (
      <>
        <div className="summary">
          <b>
            Lendo a nota… ({n} foto{n > 1 ? "s" : ""})
          </b>
          <span className="note">Costuma levar de 20 segundos a 1 minuto.</span>
        </div>
        <div className="actions">
          <button type="button" className="btn" onClick={() => fechar()}>
            Parar
          </button>
        </div>
      </>
    );
  return (
    <>
      <div className="banner">{estado.erro}</div>
      <p className="note">Dicas: nota inteira no enquadramento, sem sombra, com boa luz e de frente. Se a nota for longa, fotografe por partes e envie as fotos juntas. Se o fornecedor mandou o XML, prefira importar o XML.</p>
      <div className="actions">
        <button type="button" className="btn" onClick={() => fechar()}>
          Fechar
        </button>
        <button
          type="button"
          className="btn primary"
          onClick={() => {
            fechar();
            onOutra();
          }}
        >
          Escolher outra foto
        </button>
      </div>
    </>
  );
}
