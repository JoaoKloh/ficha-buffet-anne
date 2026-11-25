"use client";

import { useRef, useState } from "react";
import { Carregando, FormSheet, Fld, Pill, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { CATEGORIAS_PREPARACAO, PRACAS, type Praca } from "@/lib/models/dominio";
import type { CardapioLinhaDTO, EventoResponseDTO, ImportarCardapioAnaliseResponseDTO } from "@/lib/models/evento";
import { adicionarPratosCardapio, analisarCardapio, atualizarItemCardapio, confirmarCardapio, copiarCardapio, criarItemCardapio, excluirItemCardapio, opcoesEvento } from "@/lib/api/gestao/eventos";
import { dec, fmtN, numIn } from "@/lib/utils/numero";
import { filtrarPratos } from "@/lib/utils/cardapio";

const unidRend = (u: string, n: number) => (u === "porções" ? (n === 1 ? "porção" : "porções") : u === "un" ? "un" : u);

/** Adicionar prato ao cardápio, ou editar fator, praça e responsável. */
export function CardapioItemForm({ evento, linha }: { evento: EventoResponseDTO; linha?: CardapioLinhaDTO }) {
  const { dados: op, erro } = useDados(() => opcoesEvento("cardapio"));
  const c = linha?.item;
  const [v, setV] = useState({
    preparacaoId: c ? String(c.preparacaoId) : "",
    porcao: c ? dec(c.porcao) : "",
    praca: (c?.praca ?? linha?.pracaEfetiva ?? "") as Praca | "",
    responsavel: c?.responsavel ?? "",
  });
  if (!op) return <Carregando erro={erro} />;
  const info = v.preparacaoId ? op.preparacoesInfo[Number(v.preparacaoId)] : undefined;
  const hint = info ? `Em ${unidRend(info.unidadeRendimento, 2)} por convidado. A ficha rende ${fmtN(info.rendimento)} ${unidRend(info.unidadeRendimento, info.rendimento)}.` : "Escolha a preparação.";
  const dto = () => ({ eventoId: evento.id, preparacaoId: Number(v.preparacaoId), porcao: numIn(v.porcao), praca: v.praca || null, responsavel: v.responsavel });

  return (
    <FormSheet
      validar={() => (!v.preparacaoId ? "Escolha a preparação." : !(numIn(v.porcao) > 0) ? "Informe a porção por convidado." : null)}
      onSalvar={() => (c ? atualizarItemCardapio({ ...dto(), id: c.id }) : criarItemCardapio(dto()))}
      onExcluir={c ? () => excluirItemCardapio(c.id) : null}
      excluirLabel="Tirar do cardápio"
      excluirConfirma="Tirar este prato do cardápio?"
    >
      <Fld label="Preparação" full>
        <Sel
          value={v.preparacaoId}
          onChange={(x) => {
            const i = op.preparacoesInfo[Number(x)];
            setV((s) => ({ ...s, preparacaoId: x, porcao: i ? dec(i.porcaoPadrao || 1) : s.porcao, praca: i ? (i.praca as Praca) : s.praca }));
          }}
          grupos={op.preparacoes.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))}
          vazio="Escolha…"
        />
      </Fld>
      <Fld label="Fator por convidado" full hint={hint}>
        <input inputMode="decimal" value={v.porcao} onChange={(e) => setV({ ...v, porcao: e.target.value })} />
      </Fld>
      <Fld label="Praça">
        <Sel value={v.praca} onChange={(x) => setV({ ...v, praca: x as Praca })} opcoes={PRACAS.map((p) => [p, p] as const)} vazio="A da ficha" />
      </Fld>
      <Fld label="Responsável">
        <input value={v.responsavel} onChange={(e) => setV({ ...v, responsavel: e.target.value })} placeholder="Quem faz este prato" />
      </Fld>
    </FormSheet>
  );
}

/** Copia pratos e porções de outro evento. */
export function CopiarCardapioForm({ evento }: { evento: EventoResponseDTO }) {
  const { dados: op, erro } = useDados(() => opcoesEvento("copiar-cardapio"));
  const [origem, setOrigem] = useState("");
  if (!op) return <Carregando erro={erro} />;
  const outros = op.eventosComCardapio.filter((x) => x.id !== evento.id);
  if (!outros.length) return <p className="note">Nenhum outro evento tem cardápio montado ainda.</p>;
  const sel = origem || String(outros[0]!.id);
  return (
    <FormSheet intro="Os pratos e porções do evento escolhido entram neste cardápio. Depois é só ajustar." okMsg="Cardápio copiado" onSalvar={() => copiarCardapio(evento.id, Number(sel))}>
      <Fld label="Copiar de" full>
        <Sel value={sel} onChange={setOrigem} opcoes={outros.map((x) => [String(x.id), x.rotulo] as const)} />
      </Fld>
    </FormSheet>
  );
}

type LinhaImp = ImportarCardapioAnaliseResponseDTO["linhas"][number] & { prepId: string; conf: boolean; ignorar: boolean; cat: string; criar: boolean };

/** Cola o cardápio do cliente; cada linha é conferida contra as fichas antes de entrar. */
export function ImportarCardapioSheet({ evento }: { evento: EventoResponseDTO }) {
  const { fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const [texto, setTexto] = useState("");
  const [cab, setCab] = useState<string | null>(null);
  const [linhas, setLinhas] = useState<LinhaImp[] | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const upd = (ix: number, m: Partial<LinhaImp>) => setLinhas((ls) => ls!.map((l, i) => (i === ix ? { ...l, ...m } : l)));

  async function conferir() {
    if (!texto.trim()) return showToast("Cole o cardápio primeiro.", "error");
    try {
      const r = await analisarCardapio(evento.id, texto);
      setCab(r.cabecalho);
      setLinhas(r.linhas.map((l) => ({ ...l, prepId: l.situacao !== "nao" && l.sugestoes[0] ? String(l.sugestoes[0].preparacaoId) : "", conf: l.situacao === "ok", ignorar: false, cat: "volante", criar: false })));
    } catch (e) {
      showToast(msgErro(e, "Não foi possível conferir o cardápio."), "error");
    }
  }

  const pend = linhas?.filter((l) => !l.ignorar && l.situacao !== "ja" && !l.criar && (l.situacao === "nao" || (l.situacao === "parecido" && !l.conf))).length ?? 0;
  const add = linhas?.filter((l) => !l.ignorar && l.situacao !== "ja" && ((l.conf && l.prepId) || l.criar)).length ?? 0;

  async function adicionar() {
    setOcupado(true);
    try {
      const r = await confirmarCardapio(evento.id, {
        linhas: linhas!
          .filter((l) => !l.ignorar && l.situacao !== "ja" && ((l.conf && l.prepId) || l.criar))
          .map((l) => ({ texto: l.texto, preparacaoId: l.criar ? null : Number(l.prepId), criarCategoria: l.criar ? l.cat : null })),
      });
      showToast(`${r.adicionados} prato${r.adicionados === 1 ? "" : "s"} no cardápio`);
      recarregar();
      fechar();
    } catch (e) {
      showToast(msgErro(e), "error");
    } finally {
      setOcupado(false);
    }
  }

  return (
    <>
      <p className="note">Cole o cardápio como veio do cliente ou da proposta, um prato por linha. Cada linha é conferida contra as fichas: só entram pratos ligados a uma ficha. A primeira linha com data ou convidados é lida como cabeçalho.</p>
      <Fld label="Cardápio recebido" full>
        <textarea style={{ minHeight: 150 }} value={texto} onChange={(e) => setTexto(e.target.value)} placeholder={"Casamento Marina e Rafael — 13/11 — 150 convidados\nCarpaccio de carne com mostarda\nquiche alho poró\nRisoto brie e aspargos"} />
      </Fld>
      <div className="toolbar">
        <button type="button" className="btn" onClick={conferir}>
          Conferir com as fichas
        </button>
      </div>
      {linhas && (
        <>
          {cab && (
            <p className="note">
              Cabeçalho lido: <b>{cab}</b>
            </p>
          )}
          <div className="box list">
            {linhas.map((l, ix) => {
              const nomeSel = l.sugestoes.find((s) => String(s.preparacaoId) === l.prepId);
              const pill: [string, string] = l.ignorar ? ["", "Ignorado"] : l.situacao === "ja" ? ["", "Já no cardápio"] : l.criar ? ["warn", "Ficha nova (incompleta)"] : l.conf ? ["good", "Encontrado"] : l.situacao === "parecido" ? ["warn", "Parecido"] : ["bad", "Não encontrado"];
              return (
                <div key={ix} className="row static" style={{ flexWrap: "wrap", alignItems: "flex-start" }}>
                  <span className="grow" style={{ minWidth: 160 }}>
                    <span className="t wrap">{l.texto}</span>
                    {nomeSel && !l.ignorar && !l.criar && (
                      <span className="m wrap">
                        → {nomeSel.nome}
                        {nomeSel.fichaStatus !== "Completa" ? ` · ficha ${nomeSel.fichaStatus.toLowerCase()}` : ""}
                      </span>
                    )}
                  </span>
                  <Pill tom={pill[0]}>{pill[1]}</Pill>
                  {!l.ignorar && l.situacao === "parecido" && !l.conf && (
                    <div className="toolbar" style={{ width: "100%" }}>
                      <Sel className="sel-inline" ariaLabel="Ficha sugerida" value={l.prepId} onChange={(x) => upd(ix, { prepId: x })} opcoes={l.sugestoes.map((s) => [String(s.preparacaoId), s.nome] as const)} />
                      <button type="button" className="btn sm primary" onClick={() => upd(ix, { conf: true })}>
                        Confirmar
                      </button>
                      <button type="button" className="btn sm ghost" onClick={() => upd(ix, { ignorar: true })}>
                        Ignorar
                      </button>
                    </div>
                  )}
                  {!l.ignorar && l.situacao === "nao" && !l.criar && (
                    <div className="toolbar" style={{ width: "100%" }}>
                      <Sel className="sel-inline" ariaLabel="Categoria da nova ficha" value={l.cat} onChange={(x) => upd(ix, { cat: x })} opcoes={CATEGORIAS_PREPARACAO} />
                      <button type="button" className="btn sm" onClick={() => upd(ix, { criar: true })}>
                        Criar ficha
                      </button>
                      <button type="button" className="btn sm ghost" onClick={() => upd(ix, { ignorar: true })}>
                        Remover
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
          <div className="actions">
            <button type="button" className="btn" onClick={() => fechar()}>
              Cancelar
            </button>
            <button type="button" className="btn primary" disabled={!!pend || !add || ocupado} onClick={adicionar}>
              {pend ? `Resolva ${pend} pendência${pend > 1 ? "s" : ""}` : `Adicionar ${add} prato${add === 1 ? "" : "s"} ao cardápio`}
            </button>
          </div>
        </>
      )}
    </>
  );
}


/**
 * Adicionar pratos (protótipo: cardapioMultiForm): marca vários e salva uma vez só.
 * O fator por convidado vem da ficha; dá para ajustar depois na lista do cardápio.
 */
export function CardapioMultiSheet({ evento, jaNoCardapio }: { evento: EventoResponseDTO; jaNoCardapio: number[] }) {
  const { dados: op, erro } = useDados(() => opcoesEvento("cardapio"));
  const { fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const [sel, setSel] = useState<Set<number>>(new Set());
  const [busca, setBusca] = useState("");
  const [grupo, setGrupo] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const lista = useRef<HTMLDivElement>(null);
  if (!op) return <Carregando erro={erro} />;
  if (!op.preparacoes.length) return <p className="note">Cadastre as fichas técnicas primeiro, na aba Fichas.</p>;
  const ja = new Set(jaNoCardapio);
  const grupos = filtrarPratos(op.preparacoes, grupo, busca);
  // Busca ou categoria nova: a lista volta ao topo para os resultados aparecerem.
  const filtrar = (f: () => void) => {
    f();
    lista.current?.scrollTo({ top: 0 });
  };
  const marcar = (ids: number[], on: boolean) =>
    setSel((s) => {
      const n = new Set(s);
      ids.forEach((id) => (on ? n.add(id) : n.delete(id)));
      return n;
    });

  async function salvar() {
    setOcupado(true);
    try {
      const r = await adicionarPratosCardapio(evento.id, { preparacaoIds: [...sel] });
      showToast(`${r.adicionados} prato${r.adicionados === 1 ? " adicionado" : "s adicionados"} ao cardápio`);
      recarregar();
      fechar();
    } catch (e) {
      showToast(msgErro(e), "error");
      setOcupado(false);
    }
  }

  return (
    <>
      <p className="note">
        Marque todos os pratos que entram no cardápio de <b>{evento.nome}</b> e salve uma vez só. O fator por convidado vem da ficha; dá para ajustar depois na lista do cardápio.
      </p>
      <div className="toolbar">
        <input className="search" type="search" placeholder="Buscar prato…" value={busca} onChange={(e) => filtrar(() => setBusca(e.target.value))} aria-label="Buscar prato" />
      </div>
      <div className="toolbar" role="group" aria-label="Categoria">
        <button type="button" className="chip" aria-pressed={!grupo} onClick={() => filtrar(() => setGrupo(""))}>
          Todas
        </button>
        {op.preparacoes.map((g) => (
          <button key={g.grupo} type="button" className="chip" aria-pressed={grupo === g.grupo} onClick={() => filtrar(() => setGrupo(g.grupo))}>
            {g.grupo}
          </button>
        ))}
      </div>
      <div className="box rola" ref={lista}>
        {!grupos.length && <Vazio titulo="Nenhum prato encontrado">Mude a busca ou a categoria.</Vazio>}
        {grupos.map((g) => {
          const livres = g.itens.map(([id]) => id).filter((id) => !ja.has(id));
          const todos = livres.length > 0 && livres.every((id) => sel.has(id));
          return (
            <div key={g.grupo}>
              <div className="monthhead" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span>{g.grupo}</span>
                <label className="check" style={{ fontSize: 11 }}>
                  marcar todos <input type="checkbox" className="czck" checked={todos} disabled={!livres.length} onChange={(e) => marcar(livres, e.target.checked)} />
                </label>
              </div>
              {g.itens.map(([id, nome]) => {
                const tem = ja.has(id);
                const i = op.preparacoesInfo[id];
                return (
                  <label key={id} className={`row static${tem ? " off" : ""}`} style={{ cursor: tem ? "default" : "pointer" }}>
                    <input type="checkbox" className="czck" checked={tem || sel.has(id)} disabled={tem} onChange={(e) => marcar([id], e.target.checked)} />
                    <span className="grow">
                      <span className="t wrap">{nome}</span>
                      <span className="m">
                        {tem
                          ? "Já está no cardápio"
                          : i
                            ? `Fator ${fmtN(i.porcaoPadrao || 1)} ${unidRend(i.unidadeRendimento, i.porcaoPadrao || 1)} por convidado · ${i.praca}${i.temIngredientes === false ? " · ficha sem ingredientes" : ""}`
                            : ""}
                      </span>
                    </span>
                  </label>
                );
              })}
            </div>
          );
        })}
      </div>
      <div className="actions">
        <span className="note left">
          <b>{sel.size}</b> prato{sel.size === 1 ? "" : "s"} marcado{sel.size === 1 ? "" : "s"}
        </span>
        {sel.size > 0 && (
          <button type="button" className="btn sm ghost" onClick={() => setSel(new Set())}>
            Limpar
          </button>
        )}
        <button type="button" className="btn" onClick={() => fechar()}>
          Cancelar
        </button>
        <button type="button" className="btn primary" disabled={!sel.size || ocupado} onClick={salvar}>
          {sel.size ? `Adicionar ${sel.size} prato${sel.size === 1 ? "" : "s"}` : "Adicionar"}
        </button>
      </div>
    </>
  );
}
