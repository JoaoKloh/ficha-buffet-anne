"use client";

import { useState } from "react";
import { Carregando, FormSheet, Fld, Sel } from "@/components/ui/Gestao";
import { useDados } from "@/components/ui/Sheet";
import type { EventoDetalheResponseDTO } from "@/lib/models/evento";
import { atualizarContrato, excluirContrato, opcoesEvento, registrarContrato } from "@/lib/api/gestao/eventos";
import { dec, numIn } from "@/lib/utils/numero";
import { hoje, round2 } from "@/lib/utils/datas";

/** Registrar (gera parcelas e receitas previstas) ou editar o contrato do evento. */
export function ContratoForm({ det }: { det: EventoDetalheResponseDTO }) {
  const { dados: op, erro } = useDados(() => opcoesEvento("contrato"));
  const e = det.evento;
  const k = e.contrato;
  const novo = !k;
  const [v, setV] = useState({
    numero: k?.numero ?? "",
    dataAssinatura: k?.dataAssinatura ?? hoje(),
    convidados: String(k?.convidadosContratados ?? e.convidadosPrevistos ?? ""),
    porConvidado: dec(k?.valorPorConvidado || ""),
    total: dec(k?.valorTotal ?? ""),
    extra: dec(k?.valorConvidadoExtra || ""),
    categoriaId: String(k?.categoriaId ?? det.categoriaReceitaPadrao ?? ""),
    nParcelas: "3",
    primeiroVenc: hoje(),
    intervalo: "1",
  });
  const set = <K extends keyof typeof v>(key: K, x: string) =>
    setV((s) => {
      const n = { ...s, [key]: x };
      // Total calculado pelos convidados × valor por convidado; pode ajustar depois.
      if (key === "convidados" || key === "porConvidado") {
        const q = Number(n.convidados), p = numIn(n.porConvidado);
        if (q && p) n.total = round2(q * p).toFixed(2).replace(".", ",");
      }
      return n;
    });
  if (!op) return <Carregando erro={erro} />;

  const base = () => ({
    numero: v.numero,
    dataAssinatura: v.dataAssinatura,
    convidadosContratados: Math.round(Number(v.convidados)),
    valorPorConvidado: round2(numIn(v.porConvidado)),
    valorTotal: round2(numIn(v.total)),
    valorConvidadoExtra: round2(numIn(v.extra)),
    categoriaId: Number(v.categoriaId),
  });

  return (
    <FormSheet
      intro={
        novo ? (
          <>
            Ao salvar, o evento passa para <b>Contratado</b>, as parcelas são criadas e cada uma vira uma receita prevista na data do evento.
          </>
        ) : (
          `Mudanças aqui não refazem as parcelas. Ajuste cada parcela tocando nela.${det.contratoTemParcelaPaga ? " O contrato tem parcela recebida, por isso não pode ser excluído." : ""}`
        )
      }
      validar={() =>
        !(Number(v.convidados) > 0)
          ? "Informe os convidados contratados."
          : !(numIn(v.total) > 0)
            ? "Informe o valor total."
            : novo && !(Number(v.nParcelas) >= 1)
              ? "Informe ao menos uma parcela."
              : novo && !e.dataEvento
                ? "Defina a data do evento antes do contrato."
                : !v.categoriaId
                  ? "Crie uma categoria de receita em Caixa › Categorias."
                  : null
      }
      onSalvar={() =>
        novo
          ? registrarContrato(e.id, { ...base(), nParcelas: Math.round(Number(v.nParcelas)), primeiroVencimento: v.primeiroVenc, intervaloMeses: Number(v.intervalo) as 0 | 1 | 2 })
          : atualizarContrato(e.id, base())
      }
      onExcluir={!novo && !det.contratoTemParcelaPaga ? () => excluirContrato(e.id) : null}
      excluirLabel="Excluir contrato"
      excluirConfirma="Excluir o contrato e todas as parcelas e receitas previstas dele?"
    >
      <Fld label="Número do contrato">
        <input value={v.numero} onChange={(x) => set("numero", x.target.value)} placeholder="Ex.: 2026-031" />
      </Fld>
      <Fld label="Assinado em">
        <input type="date" value={v.dataAssinatura} onChange={(x) => set("dataAssinatura", x.target.value)} />
      </Fld>
      <Fld label="Convidados contratados" obrig>
        <input type="number" min={0} step={1} value={v.convidados} onChange={(x) => set("convidados", x.target.value)} />
      </Fld>
      <Fld label="Valor por convidado (R$)">
        <input inputMode="decimal" placeholder="0,00" value={v.porConvidado} onChange={(x) => set("porConvidado", x.target.value)} />
      </Fld>
      <Fld label="Valor total (R$)" obrig hint="Calculado pelos convidados; pode ajustar.">
        <input inputMode="decimal" placeholder="0,00" value={v.total} onChange={(x) => set("total", x.target.value)} />
      </Fld>
      <Fld label="Convidado extra (R$)" hint="Cobrado no acerto final.">
        <input inputMode="decimal" placeholder="0,00" value={v.extra} onChange={(x) => set("extra", x.target.value)} />
      </Fld>
      <Fld label="Categoria da receita" full>
        <Sel value={v.categoriaId} onChange={(x) => set("categoriaId", x)} grupos={[{ grupo: "Receitas", itens: op.categoriasReceita.map(([id, n]) => [String(id), n] as const) }]} vazio="Escolha…" />
      </Fld>
      {novo && (
        <>
          <Fld label="Número de parcelas" obrig>
            <input type="number" min={1} step={1} value={v.nParcelas} onChange={(x) => set("nParcelas", x.target.value)} />
          </Fld>
          <Fld label="1º vencimento" obrig>
            <input type="date" value={v.primeiroVenc} onChange={(x) => set("primeiroVenc", x.target.value)} />
          </Fld>
          <Fld label="Intervalo" full>
            <Sel value={v.intervalo} onChange={(x) => set("intervalo", x)} opcoes={[["1", "Mensal"], ["2", "A cada 2 meses"], ["0", "Mesmo dia (à vista)"]] as const} />
          </Fld>
        </>
      )}
    </FormSheet>
  );
}
