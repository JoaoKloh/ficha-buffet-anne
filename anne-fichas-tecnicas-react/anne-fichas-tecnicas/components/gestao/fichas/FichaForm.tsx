"use client";

import { useState } from "react";
import { Carregando, Fld, Pill, Sel, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { useAoVivo } from "@/components/ui/useAoVivo";
import { CATEGORIAS_PREPARACAO, CONSERVACOES, DN_OPCOES, PRACAS, SETORES_PADRAO, UNIDADES_COMPONENTE, UNIDADES_RENDIMENTO, VALIDADE_PADRAO, type CategoriaPreparacao, type Conservacao, type Praca, type UnidadeComponente, type UnidadeRendimento } from "@/lib/models/dominio";
import type { PreparacaoListaItemDTO } from "@/lib/models/preparacao";
import { atualizarPreparacao, calcularCustoFicha, criarPreparacao, excluirPreparacao, opcoesFichas } from "@/lib/api/gestao/fichas";
import { brl, dec, numIn } from "@/lib/utils/numero";
import { InsumoForm } from "./InsumoForm";

const PRACA_PADRAO: Record<CategoriaPreparacao, Praca> = { canape: "Frios", volante: "Salgados", entrada: "Frios", ilha: "Frios", principal: "Cozinha quente", acompanhamento: "Cozinha quente", sobremesa: "Confeitaria", bebida: "No evento", base: "Cozinha quente" };
const CONS_COMP: Record<Conservacao, string> = { refrigerado: "Refrigerado 0–4 °C", congelado: "Congelado", quente: "Quente", ambiente: "Ambiente" };

interface ItemD { ref: string; qtd: string; fc: string }
interface LoucaD { insumoId: string; qtd: string }
interface EtapaD { desc: string; praca: Praca | ""; dn: string }
interface CompD { n: string; u: UnidadeComponente; q: string; cons: Conservacao; obs: string }

/** Ficha técnica: rendimento, itens (insumos e sub-receitas), louças, etapas D-n e componentes de envio. */
export function FichaForm({ ficha }: { ficha?: PreparacaoListaItemDTO }) {
  const { dados: op, erro } = useDados(opcoesFichas);
  const { abrir, fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const p = ficha;
  const [d, setD] = useState({
    nome: p?.nome ?? "",
    categoria: (p?.categoria ?? "volante") as CategoriaPreparacao,
    praca: (p?.praca ?? "") as Praca | "",
    conservacao: (p?.conservacao ?? "refrigerado") as Conservacao,
    validadeDias: p?.validadeDias == null ? "" : String(p.validadeDias),
    compradoPronto: !!p?.compradoPronto,
    setor: p?.setor ?? "",
    dnProducao: p?.dnProducao == null ? "" : String(p.dnProducao),
    tempoMin: p?.tempoMin ? String(p.tempoMin) : "",
    equipamentos: p?.equipamentos ?? "",
    armazenamento: p?.armazenamento ?? "",
    rendimento: dec(p?.rendimento ?? 1),
    unidadeRendimento: (p?.unidadeRendimento ?? "porções") as UnidadeRendimento,
    porcaoPadrao: dec(p?.porcaoPadrao ?? 1),
    modoPreparo: p?.modoPreparo ?? "",
    obsEnvio: p?.obsEnvio ?? "",
  });
  const [itens, setItens] = useState<ItemD[]>(() => {
    const l = (p?.itens ?? []).map((it) => ({ ref: it.subId != null ? `s:${it.subId}` : `i:${it.insumoId}`, qtd: dec(it.qtd), fc: dec(it.fc || 1) }));
    return l.length ? l : [{ ref: "", qtd: "", fc: "1" }];
  });
  const [loucas, setLoucas] = useState<LoucaD[]>((p?.loucas ?? []).map((l) => ({ insumoId: String(l.insumoId), qtd: dec(l.qtd) })));
  const [etapas, setEtapas] = useState<EtapaD[]>((p?.etapas ?? []).map((t) => ({ desc: t.desc, praca: t.praca, dn: t.dn == null ? "" : String(t.dn) })));
  const [comps, setComps] = useState<CompD[]>((p?.componentes ?? []).map((k) => ({ n: k.n, u: k.u, q: dec(k.q), cons: k.cons, obs: k.obs ?? "" })));
  const [erroForm, setErroForm] = useState<string | null>(null);
  const [confirmando, setConfirmando] = useState(false);
  const [ocupado, setOcupado] = useState(false);

  const toItens = () =>
    itens
      .filter((it) => it.ref && it.ref !== "__novo")
      .map((it) => {
        const id = Number(it.ref.slice(2));
        return { insumoId: it.ref[0] === "i" ? id : null, subId: it.ref[0] === "s" ? id : null, qtd: numIn(it.qtd), fc: numIn(it.fc) || 1 };
      });
  const chave = JSON.stringify([toItens(), numIn(d.rendimento) || 1]);
  const custo = useAoVivo(() => calcularCustoFicha({ itens: toItens(), rendimento: numIn(d.rendimento) || 1 }), chave);
  if (!op) return <Carregando erro={erro} />;

  const set = <K extends keyof typeof d>(k: K, x: (typeof d)[K]) => setD((s) => ({ ...s, [k]: x }));
  const upd = <T,>(set: React.Dispatch<React.SetStateAction<T[]>>, ix: number, m: Partial<T>) => set((l) => l.map((x, i) => (i === ix ? { ...x, ...m } : x)));
  const praca = d.praca || PRACA_PADRAO[d.categoria];
  const porU = d.unidadeRendimento === "porções" ? "porção" : d.unidadeRendimento === "l" ? "litro" : d.unidadeRendimento;
  const subs = op.preparacoes.filter((x) => x.id !== p?.id);
  const refUnid = (ref: string) => {
    if (!ref || ref === "__novo") return "";
    const id = Number(ref.slice(2));
    return ref[0] === "i" ? op.unidades[id] ?? "" : subs.find((s) => s.id === id)?.unidade ?? "";
  };
  const grupos = [...op.insumos.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [`i:${id}`, n] as const) })), ...(subs.length ? [{ grupo: "Sub-receitas", itens: subs.map((s) => [`s:${s.id}`, `${s.nome} (${s.unidade})`] as const) }] : [])];
  const porc = numIn(d.porcaoPadrao);
  const usada = !!p?.usadaEmCardapio;
  const subDe = p?.usadaComoSub ?? [];

  async function salvar(ev: React.FormEvent) {
    ev.preventDefault();
    const its = toItens();
    const ls = loucas.filter((l) => l.insumoId).map((l) => ({ insumoId: Number(l.insumoId), qtd: numIn(l.qtd) }));
    const ets = etapas.filter((t) => t.desc.trim()).map((t) => ({ desc: t.desc.trim(), praca: t.praca, dn: t.dn === "" ? null : Math.max(0, Math.round(numIn(t.dn))) }));
    const cps = comps.filter((k) => k.n.trim()).map((k) => ({ n: k.n.trim(), u: k.u, q: numIn(k.q), cons: k.cons, obs: k.obs.trim() || undefined }));
    const e = !d.nome.trim()
      ? "Informe o nome da preparação."
      : !(numIn(d.rendimento) > 0)
        ? "Informe quanto a receita rende."
        : its.some((it) => !(it.qtd > 0))
          ? "Informe a quantidade de cada item da receita."
          : ls.some((l) => !(l.qtd > 0))
            ? "Informe a quantidade de cada louça."
            : cps.some((k) => !(k.q > 0))
              ? "Informe a quantidade de cada componente de envio."
              : null;
    if (e) return setErroForm(e);
    setErroForm(null);
    setOcupado(true);
    const dto = {
      nome: d.nome.trim(), categoria: d.categoria, praca, conservacao: d.conservacao,
      validadeDias: d.validadeDias === "" ? null : Math.max(0, Math.round(numIn(d.validadeDias))), rendimento: numIn(d.rendimento),
      compradoPronto: d.compradoPronto, setor: d.setor, dnProducao: d.dnProducao === "" ? null : Number(d.dnProducao),
      tempoMin: d.tempoMin === "" ? null : Math.max(0, Math.round(numIn(d.tempoMin))), equipamentos: d.equipamentos.trim(), armazenamento: d.armazenamento.trim(),
      unidadeRendimento: d.unidadeRendimento, porcaoPadrao: numIn(d.porcaoPadrao), itens: its, loucas: ls, etapas: ets, componentes: cps, obsEnvio: d.obsEnvio, modoPreparo: d.modoPreparo,
    };
    try {
      if (p) await atualizarPreparacao({ ...dto, id: p.id });
      else await criarPreparacao(dto);
      showToast("Ficha salva");
      recarregar();
      fechar();
    } catch (err) {
      setErroForm(msgErro(err));
    } finally {
      setOcupado(false);
    }
  }

  async function excluir() {
    try {
      await excluirPreparacao(p!.id);
      showToast("Ficha excluída");
      recarregar();
      fechar();
    } catch (err) {
      setErroForm(msgErro(err, "Não foi possível excluir."));
    }
  }

  return (
    <form className="form" noValidate onSubmit={salvar}>
      {p && (
        <div className="fld full">
          <span style={{ alignSelf: "flex-start" }}>
            <Pill tom={p.fichaStatus === "Completa" ? "good" : p.fichaStatus === "Sem preço" ? "warn" : "bad"}>Ficha {p.fichaStatus.toLowerCase()}</Pill>
          </span>
        </div>
      )}
      <Fld label="Preparação" obrig full>
        <input value={d.nome} onChange={(e) => set("nome", e.target.value)} placeholder="Ex.: Mini quiche de cogumelos" autoFocus />
      </Fld>
      <Fld label="Categoria" full hint='Use "Sub-receita" para massas, recheios, caldos e molhos usados dentro de outras fichas.'>
        <Sel value={d.categoria} onChange={(x) => set("categoria", x as CategoriaPreparacao)} opcoes={CATEGORIAS_PREPARACAO} />
      </Fld>
      <Fld label="Praça" hint="Quem prepara na cozinha.">
        <Sel value={praca} onChange={(x) => set("praca", x as Praca)} opcoes={PRACAS.map((x) => [x, x] as const)} />
      </Fld>
      <label className="check fld full" style={{ fontWeight: 500 }}>
        <input type="checkbox" checked={d.compradoPronto} onChange={(e) => setD((s) => ({ ...s, compradoPronto: e.target.checked }))} /> Comprado pronto do fornecedor (a produção não faz; só retira do estoque e finaliza)
      </label>
      <Fld label="Conservação no transporte" hint="Define a faixa de temperatura do checklist.">
        <Sel value={d.conservacao} onChange={(x) => set("conservacao", x as Conservacao)} opcoes={CONSERVACOES} />
      </Fld>
      <Fld label="Validade depois de pronta (dias)" hint="Sugere a validade dos lotes. Em branco: refrigerado 3, congelado 30, quente 0, ambiente 5.">
        <input type="number" min={0} step={1} value={d.validadeDias} placeholder={String(VALIDADE_PADRAO[d.conservacao])} onChange={(e) => set("validadeDias", e.target.value)} />
      </Fld>
      <Fld label="Setor de produção" hint="Onde a ordem de produção desta ficha é feita.">
        <Sel value={d.setor} onChange={(x) => set("setor", x)} opcoes={[["", "Automático pela praça"], ...(op?.setores ?? SETORES_PADRAO)]} />
      </Fld>
      <Fld label="Produzir com antecedência">
        <Sel value={d.dnProducao} onChange={(x) => set("dnProducao", x)} opcoes={[["", "Padrão da praça (Frios D-7 · Quentes D-6 · Salgados D-5)"], ...DN_OPCOES.map(([n, l]) => [String(n), l] as const)]} />
      </Fld>
      <Fld label="Tempo por receita (min)">
        <input type="number" min={0} step={1} value={d.tempoMin} onChange={(e) => set("tempoMin", e.target.value)} />
      </Fld>
      <Fld label="Equipamentos">
        <input value={d.equipamentos} onChange={(e) => set("equipamentos", e.target.value)} placeholder="Ex.: forno combinado, termômetro" />
      </Fld>
      <Fld label="Orientações de armazenamento" full>
        <input value={d.armazenamento} onChange={(e) => set("armazenamento", e.target.value)} placeholder="Ex.: GN tampada, etiquetar, câmara fria" />
      </Fld>
      <Fld label="A receita rende" obrig>
        <input inputMode="decimal" value={d.rendimento} onChange={(e) => set("rendimento", e.target.value)} />
      </Fld>
      <Fld label="Unidade do rendimento">
        <Sel value={d.unidadeRendimento} onChange={(x) => set("unidadeRendimento", x as UnidadeRendimento)} opcoes={UNIDADES_RENDIMENTO} />
      </Fld>
      <Fld label="Porção por convidado" full hint="Na unidade do rendimento: 1 porção, 1,5 un, 0,1 kg. Pode mudar em cada evento.">
        <input inputMode="decimal" value={d.porcaoPadrao} onChange={(e) => set("porcaoPadrao", e.target.value)} />
      </Fld>

      <div className="fld full">
        <span>Ingredientes, embalagens e sub-receitas</span>
        <div className="fitems">
          {itens.map((it, ix) => (
            <div key={ix} className="fitem">
              <Fld label="Insumo ou sub-receita">
                <Sel
                  value={it.ref}
                  onChange={(x) => {
                    if (x === "__novo")
                      return abrir("Novo item", <InsumoForm onSalvo={(i) => upd(setItens, ix, { ref: `i:${i.id}` })} />);
                    upd(setItens, ix, { ref: x });
                  }}
                  opcoes={[["__novo", "+ Novo item no cadastro"] as const]}
                  grupos={grupos}
                  vazio="Escolha…"
                />
              </Fld>
              <Fld label={`Qtd${refUnid(it.ref) ? ` (${refUnid(it.ref)})` : ""}`}>
                <input inputMode="decimal" value={it.qtd} onChange={(e) => upd(setItens, ix, { qtd: e.target.value })} />
              </Fld>
              <Fld label="FC">
                <input inputMode="decimal" value={it.fc} onChange={(e) => upd(setItens, ix, { fc: e.target.value })} />
              </Fld>
              <button type="button" className="btn sm ghost" aria-label="Remover" onClick={() => setItens((l) => (l.length > 1 ? l.filter((_, i) => i !== ix) : [{ ref: "", qtd: "", fc: "1" }]))}>
                ✕
              </button>
            </div>
          ))}
        </div>
        <div>
          <button type="button" className="btn sm" onClick={() => setItens((l) => [...l, { ref: "", qtd: "", fc: "1" }])}>
            Adicionar
          </button>
        </div>
        <small>Quantidade para uma receita, na unidade do item. FC é o fator de correção da limpeza: 1 sem perda, 1,3 compra 30% a mais. Sub-receitas se abrem nos seus ingredientes nas compras e na produção.</small>
      </div>
      <p className="note" style={{ gridColumn: "1/-1" }}>
        {custo ? (
          <>
            Custo da receita <b>{brl(custo.total)}</b> · por {porU} <b>{brl(custo.porUnid)}</b>
            {porc ? (
              <>
                {" "}
                · por convidado <b>{brl(custo.porUnid * porc)}</b>
              </>
            ) : null}
            {custo.faltando ? <span className="neg"> · {custo.faltando} sem preço</span> : null}
          </>
        ) : (
          "Calculando custo…"
        )}
      </p>

      <div className="fld full">
        <span>Louças e utensílios</span>
        <div className="fitems">
          {loucas.map((l, ix) => (
            <div key={ix} className="fitem two">
              <Fld label="Louça ou utensílio">
                <Sel value={l.insumoId} onChange={(x) => upd(setLoucas, ix, { insumoId: x })} opcoes={op.reutilizaveis.map(([id, n]) => [String(id), n] as const)} vazio="Escolha…" />
              </Fld>
              <Fld label="Por evento">
                <input inputMode="decimal" value={l.qtd} onChange={(e) => upd(setLoucas, ix, { qtd: e.target.value })} />
              </Fld>
              <button type="button" className="btn sm ghost" aria-label="Remover" onClick={() => setLoucas((x) => x.filter((_, i) => i !== ix))}>
                ✕
              </button>
            </div>
          ))}
        </div>
        <div>
          <button type="button" className="btn sm" onClick={() => setLoucas((x) => [...x, { insumoId: "", qtd: "1" }])}>
            Adicionar louça ou utensílio
          </button>
        </div>
        <small>Reutilizáveis: são reservados por data e voltam ao depósito pelo checklist. Cadastre-os em Estoque › Itens com tipo Louça ou Utensílio.</small>
      </div>

      <div className="fld full">
        <span>Modo de preparo em etapas</span>
        <div className="fitems">
          {etapas.map((t, ix) => (
            <div key={ix} className="fitem etapa">
              <Fld label={`Etapa ${ix + 1}`}>
                <input value={t.desc} onChange={(e) => upd(setEtapas, ix, { desc: e.target.value })} placeholder="Ex.: modelar, empanar e congelar" />
              </Fld>
              <Fld label="Praça">
                <Sel value={t.praca} onChange={(x) => upd(setEtapas, ix, { praca: x as Praca })} opcoes={PRACAS.map((x) => [x, x] as const)} vazio="A da ficha" />
              </Fld>
              <Fld label="D-n">
                <input inputMode="numeric" value={t.dn} placeholder="?" onChange={(e) => upd(setEtapas, ix, { dn: e.target.value })} />
              </Fld>
              <button type="button" className="btn sm ghost" aria-label="Remover etapa" onClick={() => setEtapas((x) => x.filter((_, i) => i !== ix))}>
                ✕
              </button>
            </div>
          ))}
        </div>
        <div>
          <button type="button" className="btn sm" onClick={() => setEtapas((x) => [...x, { desc: "", praca: "", dn: "" }])}>
            Adicionar etapa
          </button>
        </div>
        <small>D-n é quantos dias antes do evento a etapa acontece (0 = no dia). Em branco, a etapa entra no cronograma em &quot;A posicionar&quot;.</small>
      </div>

      <div className="fld full">
        <span>Componentes de envio (checklist)</span>
        <div className="fitems">
          {comps.map((k, ix) => (
            <div key={ix} className="fitem comp">
              <Fld label={`Componente ${ix + 1}`}>
                <input value={k.n} onChange={(e) => upd(setComps, ix, { n: e.target.value })} placeholder="Ex.: Salmão gravlax fatiado" />
              </Fld>
              <Fld label="Unid.">
                <Sel value={k.u} onChange={(x) => upd(setComps, ix, { u: x as UnidadeComponente })} opcoes={UNIDADES_COMPONENTE} />
              </Fld>
              <Fld label={`Por ${porU}`}>
                <input inputMode="decimal" value={k.q} onChange={(e) => upd(setComps, ix, { q: e.target.value })} />
              </Fld>
              <button type="button" className="btn sm ghost" aria-label="Remover componente" onClick={() => setComps((x) => x.filter((_, i) => i !== ix))}>
                ✕
              </button>
              <div className="sub2">
                <Fld label="Conservação">
                  <Sel value={k.cons} onChange={(x) => upd(setComps, ix, { cons: x as Conservacao })} opcoes={CONSERVACOES.map(([c]) => [c, CONS_COMP[c]] as const)} />
                </Fld>
                <Fld label="Observação de envio">
                  <input value={k.obs} onChange={(e) => upd(setComps, ix, { obs: e.target.value })} placeholder="Ex.: temperar no local" />
                </Fld>
              </div>
            </div>
          ))}
        </div>
        <div>
          <button type="button" className="btn sm" onClick={() => setComps((x) => [...x, { n: "", u: "kg", q: "", cons: d.conservacao === "ambiente" ? "ambiente" : "refrigerado", obs: "" }])}>
            Adicionar componente
          </button>
        </div>
        <small>O que viaja separado para montar no local (ex.: lâminas de carpaccio, molho, folhas). Quantidade por {porU} do prato; o checklist de envio multiplica pela quantidade do evento e confere cada componente em linha própria.</small>
      </div>
      <Fld label="Observação de envio do prato" full hint="Aparece na linha do prato no checklist de envio.">
        <input value={d.obsEnvio} onChange={(e) => set("obsEnvio", e.target.value)} placeholder="Ex.: empanada, fritar no local" />
      </Fld>
      <Fld label="Observações de preparo" full>
        <textarea value={d.modoPreparo} onChange={(e) => set("modoPreparo", e.target.value)} />
      </Fld>
      {subDe.length > 0 && <p className="note" style={{ gridColumn: "1/-1" }}>Usada como sub-receita em: {subDe.join(", ")}.</p>}
      {erroForm && <p className="erro-form" role="alert">{erroForm}</p>}
      <div className="actions">
        {p && !usada && !subDe.length && (
          <button type="button" className="btn danger left" onClick={() => setConfirmando(true)}>
            Excluir ficha
          </button>
        )}
        <button type="button" className="btn" onClick={() => fechar()}>
          Cancelar
        </button>
        <button type="submit" className="btn primary" disabled={ocupado}>
          Salvar ficha
        </button>
      </div>
      {confirmando && (
        <div className="confirm">
          <p>Excluir esta ficha técnica?</p>
          <button type="button" className="btn sm" onClick={() => setConfirmando(false)}>
            Cancelar
          </button>
          <button type="button" className="btn sm danger solid" onClick={excluir}>
            Excluir
          </button>
        </div>
      )}
      {usada && <p className="note" style={{ gridColumn: "1/-1" }}>Esta ficha está no cardápio de algum evento; mudanças aqui recalculam as quantidades desses eventos.</p>}
    </form>
  );
}
