"use client";

import { useSheet } from "@/components/ui/Sheet";
import type { LancamentoListaItemDTO } from "@/lib/models/caixa";
import { unCurto } from "@/lib/models/dominio";
import { brl, fdate } from "@/lib/utils/numero";
import { hoje } from "@/lib/utils/datas";
import { PagamentoSheet } from "./PagamentoSheet";
import { UnPill } from "./UnPill";

/**
 * Linha de lançamento do livro-caixa: competência, categoria, evento ou parte,
 * vencimento e o que já foi pago, unidade e o botão Pagar/Receber, que abre o
 * registro de pagamento (inteiro ou em parte). Transferência mostra origem → destino.
 */
export function LancRow({ l, onAbrir }: { l: LancamentoListaItemDTO; onAbrir: () => void }) {
  const { abrir } = useSheet();
  const r = l.grupo === "receita";
  if (l.tipo === "transferencia")
    return (
      <div className="row static">
        <button type="button" className="grow linkish" onClick={onAbrir} style={{ minWidth: 0 }}>
          <span className="t">{l.descricao || "Transferência"}</span>
          <span className="m">
            {[fdate(l.dataCompetencia), `${unCurto(l.unidadeOrigem)} → ${unCurto(l.unidadeDestino)}`, l.categoriaNome].filter(Boolean).join(" · ")}
          </span>
        </button>
        <span className="pill">transferência</span>
        <span className="amt">⇄ {brl(l.valor)}</span>
      </div>
    );
  const meta = [fdate(l.competencia), l.categoriaNome ?? "Sem categoria", l.eventoNome || l.parte || null].filter(Boolean).join(" · ");
  const v = l.vencimentoEfetivo;
  const aberto = l.status === "previsto";
  const pagar = () => abrir(r ? "Recebimento" : "Pagamento", <PagamentoSheet id={l.id} onEditar={onAbrir} />);
  return (
    <div className="row static">
      <button type="button" className="grow linkish" onClick={onAbrir} style={{ minWidth: 0 }}>
        <span className="t">{l.descricao || "(sem descrição)"}</span>
        <span className="m">
          {meta}
          {aberto && v && (v < hoje() ? <span className="neg"> · venceu {fdate(v)}</span> : ` · vence ${fdate(v)}`)}
          {aberto && l.pago > 0 && ` · ${r ? "recebido" : "pago"} ${brl(l.pago)} de ${brl(l.valor)}`}
        </span>
      </button>
      <UnPill u={l.unidadeEfetiva} />
      {aberto ? (
        <button type="button" className="btn sm" onClick={pagar} title={`Registrar ${r ? "recebimento" : "pagamento"}`}>
          {r ? "Receber" : "Pagar"}
        </button>
      ) : (
        <button type="button" className="pill good" onClick={pagar} title="Ver pagamentos">
          {r ? "recebido" : "pago"}
        </button>
      )}
      <span className={`amt${r ? " pos" : ""}`}>
        {r ? "+ " : "− "}
        {brl(aberto && l.pago > 0 ? l.saldo : l.valor)}
      </span>
    </div>
  );
}
