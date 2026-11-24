"use client";

import { useState } from "react";
import { Carregando, FormSheet, Fld, Sel, msgErro } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { CONTAS, UNIDADES, type Conta, type Unidade } from "@/lib/models/dominio";
import type { LancamentoResponseDTO } from "@/lib/models/caixa";
import { atualizarLancamento, criarLancamento, excluirLancamento } from "@/lib/api/gestao/caixa";
import { detalheEvento, opcoesEvento } from "@/lib/api/gestao/eventos";
import { brl, dec, numIn } from "@/lib/utils/numero";
import { hoje, round2, sum } from "@/lib/utils/datas";
import { ExtraForm, ParcelaForm } from "../eventos/ParcelaExtraForms";
import { TransferenciaForm } from "./TransferenciaForm";

type Preset = { eventoId?: number | null; dataCompetencia?: string; unidade?: Unidade | "" };

/** Unidade do lançamento: a gravada; sem ela, Buffet para evento e Restaurante para a conta do restaurante. */
const unidadeDe = (l: LancamentoResponseDTO): Unidade | "" => l.unidade ?? (l.eventoId != null ? "buffet" : l.conta === "banco_restaurante" ? "restaurante" : "");

/** Novo lançamento ou edição de um manual. Lançamentos de parcela e de extra abrem o formulário de origem. */
export function LancamentoForm({ lancamento, preset }: { lancamento?: LancamentoResponseDTO; preset?: Preset }) {
  const { dados: op, erro } = useDados(() => opcoesEvento("lancamento"));
  const novo = !lancamento;
  const l = lancamento;
  const pgAnt = l ? round2(sum(l.pagamentos ?? [], (p) => p.valor)) : 0;
  const [v, setV] = useState({
    descricao: l?.descricao ?? "",
    valor: dec(l?.valor ?? ""),
    categoriaId: l ? String(l.categoriaId) : "",
    eventoId: String(l?.eventoId ?? preset?.eventoId ?? ""),
    dataCompetencia: l?.dataCompetencia ?? preset?.dataCompetencia ?? hoje(),
    status: (l?.status === "previsto" ? "previsto" : l ? "realizado" : "realizado") as "previsto" | "realizado",
    vencimento: l?.vencimento ?? "",
    dataPagamento: l?.dataPagamento ?? l?.dataCompetencia ?? hoje(),
    conta: (l?.conta ?? "banco") as Conta,
    unidade: (l ? unidadeDe(l) : (preset?.unidade ?? (preset?.eventoId ? "buffet" : ""))) as Unidade | "",
    fornecedor: l?.fornecedor ?? "",
  });
  if (!op) return <Carregando erro={erro} />;
  const set = <K extends keyof typeof v>(k: K, x: (typeof v)[K]) => setV((s) => ({ ...s, [k]: x }));
  const dto = () => ({
    descricao: v.descricao.trim(),
    valor: round2(numIn(v.valor)),
    categoriaId: Number(v.categoriaId),
    eventoId: v.eventoId ? Number(v.eventoId) : null,
    dataCompetencia: v.dataCompetencia,
    status: v.status,
    vencimento: v.vencimento || null,
    dataPagamento: v.status === "realizado" ? v.dataPagamento || hoje() : null,
    conta: v.conta,
    unidade: v.unidade as Unidade,
    fornecedor: v.fornecedor.trim() || null,
  });

  return (
    <FormSheet
      validar={() =>
        !v.descricao.trim()
          ? "Descreva o lançamento."
          : !(numIn(v.valor) > 0)
            ? "Informe o valor."
            : !v.categoriaId
              ? "Escolha a categoria."
              : !v.unidade
                ? "Escolha a unidade de negócio."
                : pgAnt > numIn(v.valor) + 0.005
                  ? `Já foi pago ${brl(pgAnt)}, mais que o novo valor.`
                  : null
      }
      onSalvar={() => (l ? atualizarLancamento({ ...dto(), id: l.id }) : criarLancamento(dto()))}
      onExcluir={l ? () => excluirLancamento(l.id) : null}
    >
      <Fld label="Descrição" obrig full>
        <input value={v.descricao} onChange={(e) => set("descricao", e.target.value)} placeholder="Ex.: Hortifruti Ceasa" autoFocus />
      </Fld>
      <Fld label="Valor (R$)" obrig>
        <input inputMode="decimal" placeholder="0,00" value={v.valor} onChange={(e) => set("valor", e.target.value)} />
      </Fld>
      <Fld label="Categoria">
        <Sel value={v.categoriaId} onChange={(x) => set("categoriaId", x)} grupos={op.categoriasTodas.map((g) => ({ grupo: g.grupo, itens: g.itens.map(([id, n]) => [String(id), n] as const) }))} vazio="Escolha…" />
      </Fld>
      <Fld label="Unidade de negócio" hint="Estrutura compartilhada é rateada entre as três na DRE.">
        <Sel value={v.unidade} onChange={(x) => set("unidade", x as Unidade)} opcoes={UNIDADES} vazio="Escolha…" />
      </Fld>
      <Fld label="Fornecedor ou cliente">
        <input value={v.fornecedor} onChange={(e) => set("fornecedor", e.target.value)} placeholder="Opcional" />
      </Fld>
      <Fld label="Evento" full>
        <Sel
          value={v.eventoId}
          onChange={(x) => {
            setV((s) => ({
              ...s,
              eventoId: x,
              dataCompetencia: novo && x && op.eventosData[Number(x)] ? op.eventosData[Number(x)]! : s.dataCompetencia,
              unidade: x ? "buffet" : s.unidade,
            }));
          }}
          opcoes={op.eventos.map(([id, n]) => [String(id), n] as const)}
          vazio="Sem evento (despesa da fábrica)"
        />
      </Fld>
      <Fld label="Competência" obrig hint="Mês em que entra na DRE.">
        <input type="date" value={v.dataCompetencia} onChange={(e) => set("dataCompetencia", e.target.value)} />
      </Fld>
      <Fld label="Situação" hint={pgAnt > 0 && l?.status !== "realizado" ? `Já pago em parte: ${brl(pgAnt)}. Para registrar outra parte, use Pagar/Receber.` : undefined}>
        <Sel
          value={v.status}
          onChange={(x) => set("status", x as "previsto" | "realizado")}
          opcoes={[["realizado", "Pago / recebido"], ["previsto", "Em aberto (a pagar / a receber)"]] as const}
        />
      </Fld>
      <Fld label="Vencimento" hint="Data do boleto ou da conta." hidden={v.status === "realizado" && !l?.vencimento}>
        <input type="date" value={v.vencimento} onChange={(e) => set("vencimento", e.target.value)} />
      </Fld>
      <Fld label="Pago em" hidden={v.status !== "realizado"}>
        <input type="date" value={v.dataPagamento} onChange={(e) => set("dataPagamento", e.target.value)} />
      </Fld>
      <Fld label="Conta">
        <Sel value={v.conta} onChange={(x) => set("conta", x as Conta)} opcoes={CONTAS} />
      </Fld>
    </FormSheet>
  );
}

/** Abre o formulário certo para o lançamento: parcela, extra ou o próprio lançamento. */
export function useAbrirLancamento() {
  const { abrir } = useSheet();
  const { showToast } = useToast();
  return async (l: LancamentoResponseDTO) => {
    if (l.tipo === "transferencia") return abrir("Transferência", <TransferenciaForm transferencia={l} />);
    if ((l.origem === "parcela" || l.origem === "extra") && l.eventoId != null) {
      try {
        const det = await detalheEvento(l.eventoId);
        if (l.origem === "parcela") {
          const p = det.parcelas.find((x) => x.id === l.parcelaId);
          if (p) return abrir(`Parcela ${p.numero} · ${det.evento.nome}`, <ParcelaForm parcela={p} conta={l.conta} />);
        } else {
          const x = det.extras.find((e) => e.id === l.extraId);
          if (x) return abrir("Editar extra", <ExtraForm evento={det.evento} extra={x} />);
        }
      } catch (e) {
        showToast(msgErro(e, "Não foi possível abrir o lançamento."), "error");
        return;
      }
    }
    abrir("Editar lançamento", <LancamentoForm lancamento={l} />);
  };
}
