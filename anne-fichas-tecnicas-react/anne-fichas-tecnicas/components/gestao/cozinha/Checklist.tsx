"use client";

import { useState } from "react";
import { Fld, Pill, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import type { CozinhaProjecaoDTO, ExecucaoResponseDTO } from "@/lib/models/cozinha";
import type { EnvioExtraDTO } from "@/lib/models/evento";
import { MODOS_ENVIO_EXTRA, SECOES_ENVIO_EXTRA, UNIDADES_ENVIO_EXTRA, type ModoEnvioExtra, type SecaoEnvioExtra } from "@/lib/models/dominio";
import { lancarAvaria } from "@/lib/api/gestao/cozinha";
import { detalheEvento, patchEvento } from "@/lib/api/gestao/eventos";
import { CZ_PADRAO, CZ_REGRAS, CZ_SECS, czEnvV, czExtraQ, czFria, czGate, czRowSt, czRows, tempFora, type CkRow } from "@/lib/utils/checklist";
import { dec, fmtN, fmtQ, numIn } from "@/lib/utils/numero";
import { consLabel, exDe, parseNumBR, useCozinha, useSalvarExec } from "./comum";

type Ex = Partial<ExecucaoResponseDTO>;

/** 5 · Checklist de produto pronto: saída e retorno conferidos linha a linha. */
export function AbaChecklist({ pj }: { pj: CozinhaProjecaoDTO }) {
  const { dados, admin } = useCozinha();
  const { abrir } = useSheet();
  const rows = czRows(pj);
  const xs = rows.map((r) => [r, exDe(dados, r.id)] as const);
  const nP = rows.filter((r) => r.prod).length;
  const prod = xs.filter(([r, x]) => r.prod && (Number(x.produzido) || 0) > 0).length;
  const env = xs.filter(([r, x]) => (czEnvV(r, x) ?? 0) > 0).length;
  const tf = xs.filter(([r, x]) => czFria(r) && (tempFora(r.cons, x.tempSaida) || tempFora(r.cons, x.tempChegada))).length;
  const div = xs.filter(([r, x]) => {
    const s = czRowSt(r, x);
    return s[0] === "bad" && s[1].startsWith("Falta");
  }).length;
  const fr = xs.filter(([r]) => czFria(r));
  return (
    <>
      <p className="note">
        Conferir na saída do buffet e novamente no retorno. Toda linha precisa de duas assinaturas: <b>quem executou</b> o preparo e <b>quem separou</b> para o transporte. Linhas com <b>↳</b> são os componentes que viajam separados — cada uma é conferida individualmente. A{" "}
        <b>Qtd enviada</b> só libera com produção, as duas assinaturas e, nos itens de cadeia fria, a temperatura de saída dentro da faixa. {admin ? "A quantidade produzida do prato dá baixa dos ingredientes no estoque." : "A baixa no estoque é feita automaticamente pela gestão."} Tudo
        salva ao sair do campo.
      </p>
      {admin && (
        <div className="toolbar noprint">
          <button type="button" className="btn sm" onClick={() => abrir("Serviço, prataria e descartáveis", <ExtrasSheet eventoId={pj.eventoId} N={pj.N} base={pj.base} nome={pj.nome} />)}>
            Bebidas do serviço, prataria e descartáveis…
          </button>
          {!pj.extras.length && <span className="note">Ainda sem itens de serviço neste evento: carregue a lista padrão.</span>}
        </div>
      )}
      {!rows.length ? (
        <section className="panel">
          <Vazio titulo="Nada para enviar ainda">O checklist aparece quando a gestão marcar os pratos do cardápio como Sim.</Vazio>
        </section>
      ) : (
        <>
          <div className="kpis">
            <div className="kpi">
              <span className="k">Produzidos</span>
              <span className="v">
                {prod}/{nP}
              </span>
              <span className="h">pratos e componentes</span>
            </div>
            <div className="kpi">
              <span className="k">Enviados</span>
              <span className="v">
                {env}/{rows.length}
              </span>
              <span className="h">linhas com qtd enviada</span>
            </div>
            <div className="kpi">
              <span className="k">Cadeia fria</span>
              <span className={`v${tf ? " neg" : ""}`}>{tf ? `${tf} fora` : "ok"}</span>
              <span className="h">refrigerado 0–4 °C</span>
            </div>
            <div className="kpi">
              <span className="k">Retorno</span>
              <span className={`v${div ? " neg" : ""}`}>{div ? `${div} com falta` : "ok"}</span>
              <span className="h">louças e serviço</span>
            </div>
          </div>
          {CZ_SECS.map(([sec, rot, sub]) => {
            const l = xs.filter(([r]) => r.sec === sec);
            if (!l.length) return null;
            return (
              <section key={sec} className="panel cksec">
                <div className="panel-h">
                  <h3>
                    {rot}
                    {sub && <span className="note"> · {sub}</span>}
                  </h3>
                  <Pill>
                    {l.filter(([r, x]) => (czEnvV(r, x) ?? 0) > 0).length}/{l.length} enviados
                  </Pill>
                </div>
                <div className="tablewrap">
                  <table className="cktab">
                    <thead>
                      <tr>
                        <th>Item</th>
                        <th>Unid.</th>
                        <th className="r">Qtd prevista</th>
                        <th className="r">Qtd produzida</th>
                        <th>Executado por</th>
                        <th className="r">Qtd enviada</th>
                        <th>Separado por</th>
                        <th className="r">Qtd retorno</th>
                        <th>Conferido no retorno por</th>
                        <th>Observação</th>
                        <th>Situação</th>
                      </tr>
                    </thead>
                    <tbody>
                      {l.map(([r, x]) => (
                        <LinhaCk key={r.id} r={r} x={x} pj={pj} />
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            );
          })}
          {fr.length > 0 && (
            <section className="panel cksec">
              <div className="panel-h">
                <h3>Cadeia fria — registro de temperatura</h3>
                <span className="note">itens já listados acima, não somar de novo</span>
              </div>
              <div className="tablewrap">
                <table className="cktab" style={{ minWidth: 760 }}>
                  <thead>
                    <tr>
                      <th>Item</th>
                      <th>Unid.</th>
                      <th className="r">Qtd prevista</th>
                      <th>Faixa</th>
                      <th className="r">Temp. saída (°C)</th>
                      <th className="r">Temp. chegada (°C)</th>
                      <th>Situação</th>
                    </tr>
                  </thead>
                  <tbody>
                    {fr.map(([r, x]) => {
                      const ts = tempFora(r.cons, x.tempSaida), tc = tempFora(r.cons, x.tempChegada);
                      return (
                        <tr key={r.id}>
                          <td className="wrapc">
                            {r.nome}
                            {r.prato && <span className="note"> — {r.prato}</span>}
                          </td>
                          <td>{r.u}</td>
                          <td className="r num">{fmtQ(r.prev, r.uf || r.u)}</td>
                          <td>{consLabel(String(r.cons))}</td>
                          <td className="r">
                            <CkInput r={r} x={x} k="tempSaida" rot="Temp. saída" num temp />
                          </td>
                          <td className="r">
                            <CkInput r={r} x={x} k="tempChegada" rot="Temp. chegada" num temp />
                          </td>
                          <td>{ts || tc ? <Pill tom="bad">Fora da faixa</Pill> : ts === false ? <Pill tom="good">ok</Pill> : <Pill>Registrar</Pill>}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </section>
          )}
          <Assinaturas pj={pj} />
          <details className="ckreg">
            <summary>Regras de conferência</summary>
            <ol>
              {CZ_REGRAS.map((t) => (
                <li key={t}>{t}</li>
              ))}
            </ol>
          </details>
        </>
      )}
    </>
  );
}

function LinhaCk({ r, x, pj }: { r: CkRow; x: Ex; pj: CozinhaProjecaoDTO }) {
  const { admin } = useCozinha();
  const { recarregar } = useSheet();
  const { showToast } = useToast();
  const st = czRowSt(r, x);
  const g = czGate(r, x);
  const ev = czEnvV(r, x);
  const dis = !g && !((ev ?? 0) > 0);
  const info = [r.tipo === "prato" && r.temComp ? "componentes abaixo" : "", r.daFicha ? "insumo da ficha" : "", czFria(r) ? consLabel(String(r.cons)) : "", r.obsP || "", r.resp ? `resp. ${r.resp}` : ""].filter(Boolean).join(" · ");
  const prevTxt = fmtQ(r.prev, r.uf || r.u);
  return (
    <tr className={`ck-${r.tipo}${(ev ?? 0) > 0 ? " sent" : g ? " ready" : ""}`}>
      <td className="wrapc">
        {r.tipo === "comp" ? <span className="cksub">↳ {r.nome}</span> : <b>{r.nome}</b>}
        {info && (
          <>
            <br />
            <span className="note">{info}</span>
          </>
        )}
      </td>
      <td>{r.u}</td>
      <td className="r num">
        <b>{prevTxt}</b>
      </td>
      <td className="r">{r.prod ? <CkInput r={r} x={x} k="produzido" rot="Qtd produzida" num ph={prevTxt} /> : <span className="note">—</span>}</td>
      <td>{r.prod ? <CkInput r={r} x={x} k="executadoPor" rot="Executado por" ph={r.resp || "Nome"} /> : <span className="note">—</span>}</td>
      <td className="r">
        <CkInput r={r} x={x} k={r.envK} rot="Qtd enviada" num disabled={dis} ph={dis ? "—" : prevTxt} valor={ev} />
      </td>
      <td>
        <CkInput r={r} x={x} k="separadoPor" rot="Separado por" ph="Nome" />
      </td>
      <td className="r">
        <CkInput r={r} x={x} k={r.retK} rot="Qtd retorno" num ph="0" />
      </td>
      <td>
        <CkInput r={r} x={x} k="conferidoPor" rot="Conferido no retorno por" ph="Nome" />
      </td>
      <td>
        <CkInput r={r} x={x} k="obs" rot="Observação" wide />
      </td>
      <td>
        <Pill tom={st[0]}>{st[1]}</Pill>
        {r.tipo === "louca" && st[0] === "bad" && admin && (
          <>
            {" "}
            <button
              type="button"
              className="btn sm"
              onClick={async () => {
                try {
                  await lancarAvaria({ eventoId: pj.eventoId, insumoId: r.insumoId! });
                  showToast("Avaria lançada no estoque");
                  recarregar();
                } catch (e) {
                  showToast(msgErro(e), "error");
                }
              }}
            >
              Lançar avaria
            </button>
          </>
        )}
      </td>
    </tr>
  );
}

/** Campo do checklist: salva ao sair do campo; temperaturas mostram na hora se estão na faixa. */
function CkInput({ r, x, k, rot, num, temp, ph, disabled, wide, valor }: { r: CkRow; x: Ex; k: string; rot: string; num?: boolean; temp?: boolean; ph?: string; disabled?: boolean; wide?: boolean; valor?: number | null }) {
  const salvar = useSalvarExec();
  const { showToast } = useToast();
  const atual = valor !== undefined ? valor : (x as Record<string, unknown>)[k];
  const txt = atual == null ? "" : num ? String(atual).replace(".", ",") : String(atual);
  const [cls, setCls] = useState(() => (temp ? (tempFora(r.cons, x[k as "tempSaida"]) == null ? "" : tempFora(r.cons, x[k as "tempSaida"]) ? "bad" : "okt") : ""));
  return (
    <input
      key={`${r.id}-${k}-${txt}`}
      className={`ckin${num ? " n" : ""}${wide ? " wide" : ""}${cls ? ` ${cls}` : ""}`}
      inputMode={num ? "decimal" : undefined}
      autoComplete="off"
      defaultValue={txt}
      placeholder={ph}
      disabled={disabled}
      aria-label={`${rot}: ${r.nome}`}
      onChange={
        temp
          ? (e) => {
              const v = parseNumBR(e.target.value);
              const f = tempFora(r.cons, v ?? null);
              setCls(f == null ? "" : f ? "bad" : "okt");
            }
          : undefined
      }
      onBlur={(e) => {
        const raw = e.target.value.trim();
        if (raw === txt) return;
        let v: string | number | null = raw;
        if (num) {
          const n = parseNumBR(raw);
          if (n === undefined || (!temp && n != null && n < 0)) {
            showToast("Use só números, ex.: 3,5", "error");
            return;
          }
          v = n;
        }
        salvar({ chave: r.id, kind: r.base.kind, eventoId: r.base.eventoId, cardapioId: r.base.cardapioId ?? null, insumoId: r.base.insumoId ?? null, key: r.base.key ?? null, campos: { [k]: v } });
      }}
    />
  );
}

const ASSINATURAS: [string, string, string, string, string?, string?][] = [
  ["chefPor", "Chef responsável pela produção", "chefHora", "Data e hora"],
  ["cargaPor", "Responsável pela separação e carga", "cargaHora", "Data e hora da saída"],
  ["recebPor", "Responsável pelo recebimento no evento", "recebHora", "Data e hora da chegada"],
  ["retornoPor", "Responsável pela conferência do retorno", "retornoHora", "Data e hora do retorno"],
];

function Assinaturas({ pj }: { pj: CozinhaProjecaoDTO }) {
  const { dados } = useCozinha();
  const salvar = useSalvarExec();
  const tr = exDe(dados, `tr_${pj.eventoId}`) as Record<string, string>;
  const campo = (k: string, rot: string, ph: string) => (
    <Fld label={rot} key={k}>
      <input
        key={`${k}-${tr[k] ?? ""}`}
        defaultValue={tr[k] ?? ""}
        placeholder={ph}
        autoComplete="off"
        onBlur={(e) => e.target.value.trim() !== (tr[k] ?? "") && salvar({ chave: `tr_${pj.eventoId}`, kind: "transporte", eventoId: pj.eventoId, campos: { [k]: e.target.value.trim() } })}
      />
    </Fld>
  );
  return (
    <div className="sec">
      <h3>Assinaturas</h3>
      <div className="envcard">
        <div className="envgrid">
          {ASSINATURAS.flatMap(([k, rot, kh, roth]) => [campo(k, rot, "Nome"), campo(kh, roth, "Ex.: 20/11 14h")])}
          {campo("veiculo", "Veículo / motorista", "Ex.: van 2")}
        </div>
      </div>
    </div>
  );
}

interface ExtraD {
  k: string;
  sec: SecaoEnvioExtra;
  n: string;
  u: (typeof UNIDADES_ENVIO_EXTRA)[number];
  modo: ModoEnvioExtra;
  v: string;
  obs: string;
}

/** Gestão: bebidas do serviço, prataria e descartáveis do evento, que entram no checklist. */
function ExtrasSheet({ eventoId, N, base, nome }: { eventoId: number; N: number; base: number; nome: string }) {
  const { fechar, recarregar } = useSheet();
  const { showToast } = useToast();
  const [draft, setDraft] = useState<ExtraD[] | null>(null);
  const [ocupado, setOcupado] = useState(false);
  if (draft === null) {
    detalheEvento(eventoId).then(
      (d) => setDraft(d.evento.envioExtras.map((x: EnvioExtraDTO) => ({ k: x.k, sec: x.sec, n: x.n, u: x.u as ExtraD["u"], modo: x.modo, v: dec(x.v), obs: x.obs ?? "" }))),
      () => setDraft([])
    );
    return <div className="empty">Carregando…</div>;
  }
  const upd = (ix: number, m: Partial<ExtraD>) => setDraft((l) => l!.map((x, i) => (i === ix ? { ...x, ...m } : x)));
  const qOf = (x: ExtraD) => czExtraQ({ modo: x.modo, v: numIn(x.v), u: x.u }, { base, N });
  async function salvar(ev: React.FormEvent) {
    ev.preventDefault();
    setOcupado(true);
    try {
      await patchEvento(eventoId, { envioExtras: draft!.filter((x) => x.n.trim()).map((x) => ({ k: x.k, sec: x.sec, n: x.n.trim(), u: x.u, modo: x.modo, v: numIn(x.v) || 0, obs: x.obs.trim() || undefined })) });
      showToast("Itens do envio salvos");
      recarregar();
      fechar();
    } catch (e) {
      showToast(msgErro(e), "error");
    } finally {
      setOcupado(false);
    }
  }
  return (
    <form className="form" noValidate onSubmit={salvar}>
      <p className="note" style={{ gridColumn: "1/-1" }}>
        {nome} · {N} pessoas com margem ({base} convidados). Estes itens entram no checklist de envio da cozinha em blocos próprios, com saída e retorno conferidos linha a linha. Louças cadastradas nas fichas técnicas já entram sozinhas.
      </p>
      <div className="fld full">
        <div className="fitems">
          {draft.length ? (
            draft.map((x, ix) => (
              <div key={ix} className="fitem comp">
                <Fld label="Item">
                  <input value={x.n} onChange={(e) => upd(ix, { n: e.target.value })} />
                </Fld>
                <Fld label="Unid.">
                  <Sel value={x.u} onChange={(v) => upd(ix, { u: v as ExtraD["u"] })} opcoes={UNIDADES_ENVIO_EXTRA.map((u) => [u, u] as const)} />
                </Fld>
                <Fld label="Valor">
                  <input inputMode="decimal" value={x.v} onChange={(e) => upd(ix, { v: e.target.value })} />
                </Fld>
                <button type="button" className="btn sm ghost" aria-label="Remover item" onClick={() => setDraft((l) => l!.filter((_, i) => i !== ix))}>
                  ✕
                </button>
                <div className="sub2 tres">
                  <Fld label="Bloco">
                    <Sel value={x.sec} onChange={(v) => upd(ix, { sec: v as SecaoEnvioExtra })} opcoes={SECOES_ENVIO_EXTRA} />
                  </Fld>
                  <Fld
                    label={
                      <>
                        Cálculo ·{" "}
                        <b>
                          {fmtN(qOf(x))} {x.u}
                        </b>
                      </>
                    }
                  >
                    <Sel value={x.modo} onChange={(v) => upd(ix, { modo: v as ModoEnvioExtra })} opcoes={MODOS_ENVIO_EXTRA} />
                  </Fld>
                  <Fld label="Observação">
                    <input value={x.obs} onChange={(e) => upd(ix, { obs: e.target.value })} />
                  </Fld>
                </div>
              </div>
            ))
          ) : (
            <p className="note">Nenhum item. Use a lista padrão do modelo ou adicione um a um.</p>
          )}
        </div>
        <div className="toolbar" style={{ marginTop: 6 }}>
          <button type="button" className="btn sm" onClick={() => setDraft((l) => [...l!, { k: "", sec: "loucas", n: "", u: "un", modo: "fix", v: "1", obs: "" }])}>
            Adicionar item
          </button>
          <button
            type="button"
            className="btn sm"
            onClick={() => {
              const tem = new Set(draft.map((x) => x.n.trim().toLowerCase()));
              const novos = CZ_PADRAO.filter((r) => !tem.has(r[1].toLowerCase())).map((r) => ({ k: "", sec: r[0] as SecaoEnvioExtra, n: r[1], u: r[2] as ExtraD["u"], modo: r[3] as ModoEnvioExtra, v: dec(r[4]), obs: r[5] ?? "" }));
              setDraft((l) => [...l!, ...novos]);
              showToast(novos.length ? `${novos.length} itens da lista padrão adicionados: ajuste ao evento` : "A lista padrão já está completa");
            }}
          >
            {draft.length ? "Completar com a lista padrão" : "Carregar lista padrão"}
          </button>
        </div>
        <small>Por pessoa usa os convidados com margem; × 1,06 usa os convidados sem margem mais 6% (louças e descartáveis individuais); fixa não muda com o número de pessoas.</small>
      </div>
      <div className="actions">
        <button type="button" className="btn" onClick={() => fechar()}>
          Cancelar
        </button>
        <button type="submit" className="btn primary" disabled={ocupado}>
          Salvar
        </button>
      </div>
    </form>
  );
}
