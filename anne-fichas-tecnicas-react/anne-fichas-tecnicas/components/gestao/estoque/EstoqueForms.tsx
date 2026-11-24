"use client";

import { useState } from "react";
import { Carregando, Confirma, Datebox, FormSheet, Fld, Pill, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { LOCAIS_ESTOQUE, MOVIMENTOS_MANUAIS, TIPOS_ITEM, TIPOS_MOVIMENTO, UNIDADES, UN_OPER, rotulo, unLabel, type TipoMovimentoManual, type Unidade, type UnidadeOperacional } from "@/lib/models/dominio";
import type { MovimentoListaItemDTO } from "@/lib/models/estoque";
import { criarMovimento, excluirMovimento, itemEstoque, lancarInventario, opcoesEstoque, telaSaldo } from "@/lib/api/gestao/estoque";
import { brl, dec, fdate, fmtN, numIn, pct, qtdU } from "@/lib/utils/numero";
import { origemMovimento } from "@/lib/utils/estoque";
import { hoje, round2 } from "@/lib/utils/datas";
import { qtdConvertida } from "@/lib/utils/nf";
import { InsumoForm } from "../fichas/InsumoForm";

const LBL: Record<TipoMovimentoManual, string> = { entrada: "Quantidade que entrou", perda: "Quantidade perdida", ajuste: "Contagem real", devolucao: "Quantidade devolvida", sobra: "Quantidade que voltou", consumo: "Quantidade usada" };
const sinal = (n: number) => (n > 0 ? "+ " : n < 0 ? "− " : "");

/**
 * Lançar movimento manual: entrada (com validade), perda, ajuste de contagem, devolução, sobra
 * ou consumo de uma unidade (com transferência de custo quando outra unidade pagou a compra).
 */
export function MovForm({ tipo = "entrada", insumoId, qtd, motivo }: { tipo?: TipoMovimentoManual; insumoId?: number; qtd?: number; motivo?: string }) {
  const { dados: op, erro } = useDados(() => opcoesEstoque("movimento"));
  const [v, setV] = useState({
    tipo, data: hoje(), insumoId: insumoId ? String(insumoId) : "", emb: "", qtd: qtd != null ? dec(qtd) : "", custoUnit: "", eventoId: "", motivo: motivo ?? "",
    validade: "", unidadeUso: "restaurante" as UnidadeOperacional, compradoPor: "" as Unidade | "",
  });
  if (!op) return <Carregando erro={erro} />;
  if (!op.itens.length) return <p className="note">Cadastre os itens primeiro, em Estoque › Itens.</p>;
  const i = v.insumoId ? op.itensInfo[Number(v.insumoId)] : undefined;
  const set = <K extends keyof typeof v>(k: K, x: (typeof v)[K]) => setV((s) => ({ ...s, [k]: x }));
  const showEmb = (v.tipo === "entrada" || v.tipo === "devolucao") && !!i && Number(i.fatorCompra) > 0;
  const motivoObrig = v.tipo === "perda" || v.tipo === "ajuste" || v.tipo === "devolucao";
  const hintQtd =
    v.tipo === "ajuste"
      ? i
        ? `O sistema tem ${qtdU(i.saldo, i.unidade)}${i.saldo < 0 ? " negativos" : ""}. Informe quanto há de verdade; a diferença vira o ajuste.`
        : "Informe quanto há de verdade."
      : i
        ? `Saldo atual: ${i.saldo < 0 ? "−" : ""}${qtdU(i.saldo, i.unidade)}`
        : "";
  return (
    <FormSheet
      okMsg="Movimento lançado"
      validar={() => {
        if (!v.insumoId) return "Escolha o item.";
        if (v.tipo === "ajuste") {
          if (v.qtd.trim() === "") return "Informe a contagem real.";
        } else if (!(numIn(v.qtd) > 0)) return "Informe a quantidade.";
        if (motivoObrig && !v.motivo.trim()) return "Informe o motivo.";
        if (v.tipo === "consumo" && v.compradoPor && v.compradoPor === v.unidadeUso) return 'Quem comprou e quem usou são a mesma unidade: deixe "A mesma unidade".';
        return null;
      }}
      onSalvar={() =>
        criarMovimento({
          tipo: v.tipo, insumoId: Number(v.insumoId), data: v.data || hoje(), qtd: numIn(v.qtd), custoUnit: v.tipo === "entrada" && numIn(v.custoUnit) > 0 ? round2(numIn(v.custoUnit)) : null,
          eventoId: (v.tipo === "sobra" || v.tipo === "perda") && v.eventoId ? Number(v.eventoId) : null, motivo: v.motivo.trim(),
          validade: v.tipo === "entrada" && v.validade ? v.validade : null,
          unidadeUso: v.tipo === "consumo" ? v.unidadeUso : null,
          compradoPor: v.tipo === "consumo" && v.compradoPor ? v.compradoPor : null,
        })
      }
    >
      <Fld label="Tipo">
        <Sel value={v.tipo} onChange={(x) => set("tipo", x as TipoMovimentoManual)} opcoes={MOVIMENTOS_MANUAIS.map((k) => [k, TIPOS_MOVIMENTO[k].rotulo] as const)} />
      </Fld>
      <Fld label="Data" obrig>
        <input type="date" value={v.data} onChange={(e) => set("data", e.target.value)} />
      </Fld>
      <Fld label="Item" full>
        <Sel value={v.insumoId} onChange={(x) => set("insumoId", x)} grupos={op.itens.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))} vazio="Escolha…" />
      </Fld>
      <Fld label="Embalagens de compra" hidden={!showEmb} hint={i ? `${i.unidadeCompra || "embalagem"} = ${fmtN(i.fatorCompra)} ${i.unidade}. Preenche a quantidade sozinho.` : undefined}>
        <input
          inputMode="decimal"
          value={v.emb}
          onChange={(e) => {
            const n = numIn(e.target.value);
            setV((s) => ({ ...s, emb: e.target.value, qtd: n && i ? String(qtdConvertida(n, "compra", i.fatorCompra)).replace(".", ",") : "" }));
          }}
        />
      </Fld>
      <Fld label={`${LBL[v.tipo]}${i ? ` (${i.unidade})` : ""}`} obrig hint={hintQtd}>
        <input inputMode="decimal" value={v.qtd} onChange={(e) => set("qtd", e.target.value)} />
      </Fld>
      <Fld label="Preço pago por unidade de estoque (R$)" hidden={v.tipo !== "entrada"} hint="Opcional. Atualiza o preço do item e o custo das fichas.">
        <input inputMode="decimal" placeholder="0,00" value={v.custoUnit} onChange={(e) => set("custoUnit", e.target.value)} />
      </Fld>
      <Fld label="Validade" hidden={v.tipo !== "entrada"} hint="Para perecíveis: aparece em Estoque › Validades.">
        <input type="date" value={v.validade} onChange={(e) => set("validade", e.target.value)} />
      </Fld>
      <Fld label="Usado por" hidden={v.tipo !== "consumo"}>
        <Sel value={v.unidadeUso} onChange={(x) => set("unidadeUso", x as UnidadeOperacional)} opcoes={UN_OPER.map((u) => [u, unLabel(u)] as const)} />
      </Fld>
      <Fld label="Comprado por" hidden={v.tipo !== "consumo"} hint="Se outra unidade pagou a compra, o custo é transferido para quem usou (Caixa › Transferências).">
        <Sel value={v.compradoPor} onChange={(x) => set("compradoPor", x as Unidade | "")} opcoes={[["", "A mesma unidade (sem transferência)"], ...UNIDADES]} />
      </Fld>
      <Fld label="Evento" full hidden={!(v.tipo === "sobra" || v.tipo === "perda")}>
        <Sel value={v.eventoId} onChange={(x) => set("eventoId", x)} opcoes={op.eventos.map(([id, n]) => [String(id), n] as const)} vazio="Sem evento" />
      </Fld>
      <Fld label="Motivo" obrig={motivoObrig} full>
        <input value={v.motivo} onChange={(e) => set("motivo", e.target.value)} placeholder="Ex.: vencido, quebra, contagem de sexta" />
      </Fld>
    </FormSheet>
  );
}

export function MovRow({ m, onClick }: { m: MovimentoListaItemDTO; onClick?: () => void }) {
  const t = TIPOS_MOVIMENTO[m.tipo] ?? { rotulo: m.tipo };
  const conteudo = (
    <>
      <Datebox iso={m.data} />
      <span className="grow">
        <span className="t">{m.insumoNome}</span>
        <span className="m">{[t.rotulo, origemMovimento(m), m.motivo, m.eventoNome, m.validade ? `validade ${fdate(m.validade)}` : null].filter(Boolean).join(" · ")}</span>
      </span>
      <span className={`amt ${m.qtd > 0 ? "pos" : m.qtd < 0 ? "neg" : ""}`}>
        {sinal(m.qtd)}
        {qtdU(m.qtd, m.unidade)}
      </span>
    </>
  );
  return m.virtual || !onClick ? (
    <div className="row static">{conteudo}</div>
  ) : (
    <button type="button" className="row" onClick={onClick}>
      {conteudo}
    </button>
  );
}

/** Detalhe de um movimento gravado, com exclusão (o saldo volta a ser o de antes). */
export function MovSheet({ m }: { m: MovimentoListaItemDTO }) {
  const { fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const [conf, setConf] = useState(false);
  const n = m.qtd;
  return (
    <>
      <dl className="facts">
        <div>
          <dt>Item</dt>
          <dd>{m.insumoNome}</dd>
        </div>
        <div>
          <dt>Data</dt>
          <dd>{fdate(m.data)}</dd>
        </div>
        <div>
          <dt>Efeito no saldo</dt>
          <dd className={`num ${n > 0 ? "pos" : n < 0 ? "neg" : ""}`}>
            {sinal(n)}
            {qtdU(n, m.unidade)}
          </dd>
        </div>
        {(m.tipo === "ajuste" || m.tipo === "inventario") && (
          <div>
            <dt>Contagem informada</dt>
            <dd className="num">{qtdU(m.qtdInformada || 0, m.unidade)}</dd>
          </div>
        )}
        {m.custoUnit != null && (
          <div>
            <dt>Preço pago</dt>
            <dd className="num">
              {brl(m.custoUnit)} /{m.unidade}
            </dd>
          </div>
        )}
        {m.eventoNome && (
          <div>
            <dt>Evento</dt>
            <dd>{m.eventoNome}</dd>
          </div>
        )}
        {m.motivo && (
          <div style={{ gridColumn: "1/-1" }}>
            <dt>Motivo</dt>
            <dd>{m.motivo}</dd>
          </div>
        )}
      </dl>
      <div className="actions" style={{ justifyContent: "flex-start" }}>
        <button type="button" className="btn danger" onClick={() => setConf(true)}>
          Excluir movimento
        </button>
      </div>
      {conf && (
        <Confirma
          msg="Excluir este movimento? O saldo volta a ser o de antes dele."
          rotulo="Excluir"
          onNao={() => setConf(false)}
          onSim={async () => {
            try {
              await excluirMovimento(m.id);
              showToast("Movimento excluído");
              recarregar();
              fechar();
            } catch (e) {
              showToast(msgErro(e), "error");
            }
          }}
        />
      )}
    </>
  );
}

/** Contagem de inventário: só os itens contados com diferença viram movimento. */
export function InventarioSheet({ inicial }: { inicial: boolean }) {
  const { dados, erro } = useDados(telaSaldo);
  const { fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const [draft, setDraft] = useState<Record<number, string>>({});
  const [local, setLocal] = useState("");
  const [busca, setBusca] = useState("");
  const [ocupado, setOcupado] = useState(false);
  if (!dados) return <Carregando erro={erro} />;
  const q = busca.trim().toLowerCase();
  const its = dados.itens.filter((x) => (!local || x.insumo.local === local) && (!q || x.insumo.nome.toLowerCase().includes(q)));
  const difere = (id: number) => {
    const v = draft[id];
    const x = dados.itens.find((y) => y.insumo.id === id);
    return v != null && v !== "" && !!x && Math.abs(numIn(v) - x.fisico) > 1e-9;
  };
  const alterados = Object.keys(draft).map(Number).filter(difere);

  async function lancar() {
    setOcupado(true);
    try {
      const r = await lancarInventario({ contagens: alterados.map((id) => ({ insumoId: id, contado: numIn(draft[id]) })) });
      showToast(`${r.lancados} ite${r.lancados === 1 ? "m lançado" : "ns lançados"} no inventário`);
      recarregar();
      fechar();
    } catch (e) {
      showToast(msgErro(e), "error");
      setOcupado(false);
    }
  }

  return (
    <>
      <p className="note">{inicial ? "Conte o que existe hoje na cozinha, câmara e depósito e preencha a coluna Contado. Os itens deixados em branco ficam como estão." : "Preencha só o que foi contado. A diferença para o sistema vira um movimento de inventário."}</p>
      <div className="toolbar">
        <Sel className="sel-inline" ariaLabel="Local" value={local} onChange={setLocal} opcoes={LOCAIS_ESTOQUE.map((l) => [l, l] as const)} vazio="Todos os locais" />
        <input className="search" type="search" placeholder="Buscar item" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar item" />
      </div>
      <div className="box tablewrap">
        <table>
          <thead>
            <tr>
              <th>Item</th>
              <th className="r">Sistema</th>
              <th className="r">Contado</th>
            </tr>
          </thead>
          <tbody>
            {its.map((x, i) => (
              <InvLinha key={x.insumo.id} x={x} cab={i === 0 || its[i - 1]!.insumo.local !== x.insumo.local} valor={draft[x.insumo.id] ?? ""} mudou={difere(x.insumo.id)} onChange={(v) => setDraft((d) => ({ ...d, [x.insumo.id]: v.trim() }))} />
            ))}
          </tbody>
        </table>
      </div>
      <div className="actions">
        <span className="note left">
          {alterados.length} ite{alterados.length === 1 ? "m" : "ns"} com diferença
        </span>
        <button type="button" className="btn" onClick={() => fechar()}>
          Cancelar
        </button>
        <button type="button" className="btn primary" disabled={!alterados.length || ocupado} onClick={lancar}>
          {ocupado ? "Lançando…" : "Lançar inventário"}
        </button>
      </div>
    </>
  );
}

function InvLinha({ x, cab, valor, mudou, onChange }: { x: { insumo: { id: number; nome: string; unidade: string; local: string }; fisico: number }; cab: boolean; valor: string; mudou: boolean; onChange: (v: string) => void }) {
  return (
    <>
      {cab && (
        <tr>
          <td colSpan={3} className="grp">
            {x.insumo.local}
          </td>
        </tr>
      )}
      <tr>
        <td className="wrapc">
          <b>{x.insumo.nome}</b>
          <br />
          <span className="note">{x.insumo.unidade}</span>
        </td>
        <td className="r">
          {x.fisico < 0 ? "−" : ""}
          {qtdU(x.fisico, x.insumo.unidade)}
        </td>
        <td className="r">
          <input className={`stock-input${mudou ? " changed" : ""}`} inputMode="decimal" aria-label={`Contado de ${x.insumo.nome}`} value={valor} onChange={(e) => onChange(e.target.value)} />
        </td>
      </tr>
    </>
  );
}

/** Ficha do item no estoque: saldo, reservas, atalhos de movimento e histórico. */
export function ItemSheet({ id }: { id: number }) {
  const { dados: x, erro } = useDados(() => itemEstoque(id), [id]);
  const { abrir } = useSheet();
  if (!x) return <Carregando erro={erro} />;
  const i = x.insumo;
  const reut = i.tipo === "louca" || i.tipo === "utensilio";
  return (
    <>
      <div className="sec">
        <div className="toolbar between">
          <Pill>{rotulo(TIPOS_ITEM, i.tipo, "Item")}</Pill>
          <Pill tom={x.situacao[0]}>{x.situacao[1]}</Pill>
        </div>
        <h2>{i.nome}</h2>
      </div>
      <dl className="facts">
        <div>
          <dt>{reut ? "Total" : "Físico"}</dt>
          <dd className={`num${x.fisico < 0 ? " neg" : ""}`}>
            {x.fisico < 0 ? "−" : ""}
            {qtdU(x.fisico, i.unidade)}
          </dd>
        </div>
        <div>
          <dt>Mínimo</dt>
          <dd className="num">{i.estoqueMinimo > 0 ? qtdU(i.estoqueMinimo, i.unidade) : "—"}</dd>
        </div>
        <div>
          <dt>Local</dt>
          <dd>{i.local}</dd>
        </div>
        <div>
          <dt>Preço</dt>
          <dd className="num">{i.custo > 0 ? `${brl(i.custo)} /${i.unidade}` : "sem preço"}</dd>
        </div>
        <div>
          <dt>Compra</dt>
          <dd>
            {i.unidadeCompra || "—"}
            {i.fatorCompra ? ` = ${fmtN(i.fatorCompra)} ${i.unidade}` : ""}
          </dd>
        </div>
        <div>
          <dt>Valor em estoque</dt>
          <dd className="num">{x.valor ? brl(x.valor) : "—"}</dd>
        </div>
        {reut ? (
          <div>
            <dt>Fora em eventos</dt>
            <dd className="num">{fmtN(x.fora)}</dd>
          </div>
        ) : (
          <>
            <div>
              <dt>Reservado</dt>
              <dd className="num">{qtdU(x.reservado, i.unidade)}</dd>
            </div>
            <div>
              <dt>Disponível</dt>
              <dd className={`num${x.disponivel < 0 ? " neg" : ""}`}>
                {x.disponivel < 0 ? "−" : ""}
                {qtdU(x.disponivel, i.unidade)}
              </dd>
            </div>
          </>
        )}
      </dl>
      {x.reservas.length > 0 && <p className="note">Reservas: {x.reservas.map((r) => `${r.codigo} · ${r.nome} (${qtdU(r.qtd, i.unidade)})`).join(", ")}.</p>}
      <div className="toolbar">
        <button type="button" className="btn sm primary" onClick={() => abrir("Lançar movimento", <MovForm tipo="entrada" insumoId={id} />)}>
          Entrada
        </button>
        <button type="button" className="btn sm" onClick={() => abrir("Lançar movimento", <MovForm tipo="perda" insumoId={id} />)}>
          Perda
        </button>
        <button type="button" className="btn sm" onClick={() => abrir("Lançar movimento", <MovForm tipo="ajuste" insumoId={id} />)}>
          Ajustar contagem
        </button>
        <button type="button" className="btn sm" onClick={() => abrir("Lançar movimento", <MovForm tipo="sobra" insumoId={id} />)}>
          Sobra
        </button>
        <button type="button" className="btn sm ghost" onClick={() => abrir("Editar item", <InsumoForm insumo={i} />)}>
          Editar cadastro
        </button>
      </div>
      {x.usadoEm.length > 0 && <p className="note">Usado em: {x.usadoEm.join(", ")}.</p>}
      {x.precos && x.precos.entradas.length > 0 && <PrecosItem p={x.precos} unidade={i.unidade} />}
      <div className="sec">
        <h3>Movimentos</h3>
        <div className="box list">{x.movimentos.length ? x.movimentos.map((m) => <MovRow key={m.id} m={m} onClick={() => abrir("Movimento", <MovSheet m={m} />)} />) : <Vazio>Nenhum movimento deste item.</Vazio>}</div>
      </div>
    </>
  );
}

/** Preços e fornecedores do item: variação desde a primeira compra e últimas entradas. */
function PrecosItem({ p, unidade }: { p: NonNullable<import("@/lib/models/estoque").ItemEstoqueDetalheDTO["precos"]>; unidade: string }) {
  return (
    <div className="sec">
      <div className="sec-h">
        <h3>Preços e fornecedores</h3>
        <span className="note">{p.desde && p.variacao != null ? `desde ${fdate(p.desde)}: ${p.variacao >= 0 ? "+" : ""}${pct(p.variacao)}` : ""}</span>
      </div>
      <div className="summary">
        {p.fornecedores.map((f) => (
          <span key={f.fornecedor}>
            <b>{f.fornecedor}</b>: {f.compras} compra{f.compras > 1 ? "s" : ""} · último {brl(f.ultimo)} · menor {brl(f.menor)}
          </span>
        ))}
      </div>
      <div className="box list">
        {p.entradas.map((x, ix) => (
          <div key={ix} className="row static">
            <Datebox iso={x.data} />
            <span className="grow">
              <span className="t">{x.fornecedor || "Sem fornecedor"}</span>
              <span className="m">{qtdU(x.qtd, unidade)}</span>
            </span>
            {x.variacao != null && Math.abs(x.variacao) >= 0.005 && (
              <Pill tom={x.variacao > 0.05 ? "bad" : x.variacao < 0 ? "good" : ""}>
                {x.variacao > 0 ? "+" : ""}
                {pct(x.variacao)}
              </Pill>
            )}
            <span className="amt">
              {brl(x.custoUnit)}
              <span className="m" style={{ display: "inline" }}>
                {" "}
                /{unidade}
              </span>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
