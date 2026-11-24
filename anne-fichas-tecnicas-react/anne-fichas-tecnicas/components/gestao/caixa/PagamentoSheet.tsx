"use client";

import { useState } from "react";
import { Carregando, Confirma, Datebox, Fld, Pill, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { desfazerPagamento, obterLancamento, registrarPagamento } from "@/lib/api/gestao/caixa";
import { CONTAS, FORMAS_PAGAMENTO_NF, rotulo, type Conta, type FormaPagamentoNf } from "@/lib/models/dominio";
import { brl, dec, fdate, numIn } from "@/lib/utils/numero";
import { hoje, round2 } from "@/lib/utils/datas";
import { UnPill } from "./UnPill";

const fdh = (iso: string | null) => (iso ? `${fdate(iso.slice(0, 10))} às ${new Date(iso).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}` : "");

/**
 * Pagamento ou recebimento de um lançamento: registra o saldo inteiro ou só uma
 * parte, mostra o histórico (data, conta, forma, quem e quando registrou) e
 * desfaz o último.
 */
export function PagamentoSheet({ id, onEditar }: { id: number; onEditar?: () => void }) {
  const { dados: l, erro } = useDados(() => obterLancamento(id), [id]);
  const { recarregar, fechar } = useSheet();
  const { showToast } = useToast();
  const [form, setForm] = useState<{ valor: string; data: string; conta: Conta | ""; forma: FormaPagamentoNf | "" } | null>(null);
  const [erroForm, setErroForm] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  if (!l) return <Carregando erro={erro} />;
  const r = l.grupo === "receita";
  const f = form ?? {
    valor: dec(l.saldo.toFixed(2)),
    data: hoje(),
    conta: (l.conta || (l.unidadeEfetiva === "restaurante" ? "banco_restaurante" : "banco")) as Conta,
    forma: (l.formaPag ?? "") as FormaPagamentoNf | "",
  };
  const set = (p: Partial<typeof f>) => setForm({ ...f, ...p });

  async function registrar() {
    const v = round2(numIn(f.valor));
    if (!(v > 0)) return setErroForm("Informe o valor.");
    if (v > l!.saldo + 0.005) return setErroForm(`O valor passa do saldo (${brl(l!.saldo)}).`);
    if (!f.data) return setErroForm("Informe a data.");
    setErroForm(null);
    setOcupado(true);
    try {
      await registrarPagamento(id, { valor: v, data: f.data, conta: (f.conta || "banco") as Conta, forma: f.forma });
      showToast(v < l!.saldo - 0.005 ? `${r ? "Recebimento" : "Pagamento"} parcial registrado · saldo ${brl(l!.saldo - v)}` : r ? "Recebido" : "Pago");
      recarregar();
      fechar();
    } catch (e) {
      setErroForm(msgErro(e));
    } finally {
      setOcupado(false);
    }
  }
  async function desfazer() {
    setOcupado(true);
    try {
      await desfazerPagamento(id);
      showToast("Desfeito");
      setConfirmando(false);
      setForm(null);
      recarregar();
    } catch (e) {
      showToast(msgErro(e), "error");
    } finally {
      setOcupado(false);
    }
  }

  return (
    <>
      <div className="sec">
        <div className="toolbar between">
          <UnPill u={l.unidadeEfetiva} />
          <Pill tom={l.situacaoTom}>{l.situacao}</Pill>
        </div>
        <h2>{l.descricao || "(sem descrição)"}</h2>
        {l.parte && <p className="note">{l.parte}</p>}
      </div>
      <div className="result">
        <div>
          <div className="k">Valor</div>
          <div className="v">{brl(l.valor)}</div>
        </div>
        <div>
          <div className="k">{r ? "Recebido" : "Pago"}</div>
          <div className="v">{brl(l.pago)}</div>
        </div>
        <div>
          <div className="k">Saldo</div>
          <div className={`v ${l.saldo > 0 ? "neg" : "pos"}`}>{brl(l.saldo)}</div>
        </div>
        <div>
          <div className="k">Vencimento</div>
          <div className="v">{fdate(l.vencimentoEfetivo)}</div>
        </div>
      </div>
      {l.saldo > 0 && (
        <div className="form">
          <Fld label={`Valor ${r ? "recebido" : "pago"} agora (R$)`} obrig hint="Deixe o saldo inteiro ou troque por uma parte.">
            <input inputMode="decimal" autoComplete="off" value={f.valor} onChange={(e) => set({ valor: e.target.value })} />
          </Fld>
          <Fld label="Data" obrig>
            <input type="date" value={f.data} onChange={(e) => set({ data: e.target.value })} />
          </Fld>
          <Fld label="Conta">
            <Sel value={f.conta} onChange={(x) => set({ conta: x as Conta })} opcoes={CONTAS} />
          </Fld>
          <Fld label="Forma">
            <Sel value={f.forma} onChange={(x) => set({ forma: x as FormaPagamentoNf })} opcoes={FORMAS_PAGAMENTO_NF} vazio="—" />
          </Fld>
          {erroForm && (
            <p className="erro-form" role="alert">
              {erroForm}
            </p>
          )}
          <div className="actions">
            <button type="button" className="btn" onClick={() => fechar()}>
              Cancelar
            </button>
            <button type="button" className="btn primary" onClick={registrar} disabled={ocupado}>
              Registrar {r ? "recebimento" : "pagamento"}
            </button>
          </div>
        </div>
      )}
      <div className="sec">
        <h3>Histórico</h3>
        <div className="box list">
          {l.pagamentos.length ? (
            l.pagamentos.map((p, ix) => (
              <div key={p.id ?? ix} className="row static">
                <Datebox iso={p.data} />
                <span className="grow">
                  <span className="t">
                    {ix + 1}º {r ? "recebimento" : "pagamento"}
                  </span>
                  <span className="m">
                    {[rotulo(CONTAS, p.conta, p.conta), p.forma ? rotulo(FORMAS_PAGAMENTO_NF, p.forma, p.forma) : null, p.em ? `registrado em ${fdh(p.em)}` : null, p.por || null]
                      .filter(Boolean)
                      .join(" · ")}
                  </span>
                </span>
                <span className="amt">{brl(p.valor)}</span>
              </div>
            ))
          ) : (
            <Vazio>Nada {r ? "recebido" : "pago"} ainda.</Vazio>
          )}
        </div>
        {l.pagamentos.length > 0 && !confirmando && (
          <div className="toolbar">
            <button type="button" className="btn sm danger" onClick={() => setConfirmando(true)}>
              Desfazer o último
            </button>
          </div>
        )}
      </div>
      {confirmando && (
        <Confirma
          msg={`Desfazer o último ${r ? "recebimento" : "pagamento"}? A conta volta a ficar em aberto com o saldo correspondente.`}
          onSim={desfazer}
          onNao={() => setConfirmando(false)}
          rotulo="Desfazer"
        />
      )}
      {(l.criadoPor || l.alteradoPor) && (
        <p className="note">
          {[l.criadoPor ? `Criado por ${l.criadoPor}` : null, l.alteradoPor && l.alteradoEm ? `alterado por ${l.alteradoPor} em ${fdh(l.alteradoEm)}` : null]
            .filter(Boolean)
            .join(" · ")}
        </p>
      )}
      {onEditar && (
        <div className="toolbar">
          <button type="button" className="btn sm ghost" onClick={onEditar}>
            Editar lançamento
          </button>
        </div>
      )}
    </>
  );
}
