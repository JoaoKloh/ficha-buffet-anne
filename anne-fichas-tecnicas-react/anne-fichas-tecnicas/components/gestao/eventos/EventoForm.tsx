"use client";

import { useState } from "react";
import { Carregando, FormSheet, Fld, Sel } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { STATUS_EVENTO, TIPOS_EVENTO, type StatusEvento, type TipoEvento } from "@/lib/models/dominio";
import type { EventoResponseDTO } from "@/lib/models/evento";
import { atualizarEvento, criarEvento, excluirEvento, opcoesEvento } from "@/lib/api/gestao/eventos";
import { dec, numIn } from "@/lib/utils/numero";
import { EventoSheet } from "./EventoSheet";

/** Novo evento (nasce como lead) ou edição. O cliente pode ser criado aqui mesmo. */
export function EventoForm({ evento, travado }: { evento?: EventoResponseDTO; travado?: string }) {
  const { dados: op, erro } = useDados(() => opcoesEvento("evento"));
  const { fechar, abrir } = useSheet();
  const novo = !evento;
  const [v, setV] = useState({
    nome: evento?.nome ?? "",
    clienteId: evento ? String(evento.clienteId) : "",
    cliNome: "",
    cliTel: "",
    tipo: (evento?.tipo ?? "casamento") as TipoEvento,
    status: (evento?.status ?? "lead") as StatusEvento,
    dataEvento: evento?.dataEvento ?? "",
    horaInicio: evento?.horaInicio ?? "",
    local: evento?.local ?? "",
    cidade: evento?.cidade ?? "",
    convidados: evento?.convidadosPrevistos != null ? String(evento.convidadosPrevistos) : "",
    margem: evento ? dec(evento.margem) : "5",
    observacoes: evento?.observacoes ?? "",
  });
  const set = <K extends keyof typeof v>(k: K, x: (typeof v)[K]) => setV((s) => ({ ...s, [k]: x }));
  if (!op) return <Carregando erro={erro} />;
  const cliNovo = v.clienteId === "__novo" || (!op.clientes.length && !v.clienteId);

  const dto = () => ({
    nome: v.nome.trim(),
    clienteId: cliNovo ? null : Number(v.clienteId),
    novoCliente: cliNovo ? { nome: v.cliNome.trim(), telefone: v.cliTel.trim() } : null,
    tipo: v.tipo,
    status: v.status,
    dataEvento: v.dataEvento,
    horaInicio: v.horaInicio,
    local: v.local,
    cidade: v.cidade,
    convidadosPrevistos: v.convidados ? Math.round(Number(v.convidados)) || null : null,
    margem: Math.max(0, numIn(v.margem)),
    observacoes: v.observacoes,
  });

  return (
    <FormSheet
      intro={travado ? `Este evento tem ${travado}, por isso não pode ser excluído: isso apagaria o histórico do caixa e do estoque. Para tirá-lo da agenda, mude a etapa para Cancelado ou Perdido.` : undefined}
      validar={() => (!v.nome.trim() ? "Informe o nome do evento." : !cliNovo && !v.clienteId ? "Escolha o cliente ou cadastre um novo." : cliNovo && !v.cliNome.trim() ? "Informe o nome do novo cliente." : null)}
      onSalvar={() => (evento ? atualizarEvento({ ...dto(), id: evento.id }) : criarEvento(dto()))}
      depois={
        novo
          ? (r) => {
              const e = r as EventoResponseDTO;
              fechar(true);
              abrir(e.nome, <EventoSheet id={e.id} />);
            }
          : undefined
      }
      onExcluir={!novo && !travado ? () => excluirEvento(evento!.id) : null}
      depoisExcluir={() => fechar(true)}
      excluirLabel="Excluir evento"
      excluirConfirma="Excluir o evento com contrato, parcelas, extras e lançamentos ligados a ele?"
    >
      <Fld label="Nome do evento" obrig full>
        <input value={v.nome} onChange={(e) => set("nome", e.target.value)} placeholder="Ex.: Isabela & Jair" autoFocus />
      </Fld>
      <Fld label="Cliente" full>
        <Sel value={cliNovo ? "__novo" : v.clienteId} onChange={(x) => set("clienteId", x)} opcoes={[["__novo", "+ Novo cliente"] as const, ...op.clientes.map(([id, n]) => [String(id), n] as const)]} vazio={op.clientes.length ? "Escolha…" : undefined} />
      </Fld>
      <Fld label="Nome do cliente" hidden={!cliNovo}>
        <input value={v.cliNome} onChange={(e) => set("cliNome", e.target.value)} placeholder="Quem contrata" />
      </Fld>
      <Fld label="Telefone / WhatsApp" hidden={!cliNovo}>
        <input type="tel" value={v.cliTel} onChange={(e) => set("cliTel", e.target.value)} />
      </Fld>
      <Fld label="Tipo">
        <Sel value={v.tipo} onChange={(x) => set("tipo", x as TipoEvento)} opcoes={TIPOS_EVENTO} />
      </Fld>
      <Fld label="Etapa">
        <Sel value={v.status} onChange={(x) => set("status", x as StatusEvento)} opcoes={STATUS_EVENTO} />
      </Fld>
      <Fld label="Data">
        <input type="date" value={v.dataEvento} onChange={(e) => set("dataEvento", e.target.value)} />
      </Fld>
      <Fld label="Horário">
        <input type="time" value={v.horaInicio} onChange={(e) => set("horaInicio", e.target.value)} />
      </Fld>
      <Fld label="Local">
        <input value={v.local} onChange={(e) => set("local", e.target.value)} placeholder="Espaço ou endereço" />
      </Fld>
      <Fld label="Cidade">
        <input value={v.cidade} onChange={(e) => set("cidade", e.target.value)} placeholder="Petrópolis" />
      </Fld>
      <Fld label="Convidados previstos">
        <input type="number" inputMode="numeric" min={0} step={1} value={v.convidados} onChange={(e) => set("convidados", e.target.value)} />
      </Fld>
      <Fld label="Margem de produção (%)" hint="Produção, compras e reservas calculam convidados + margem.">
        <input inputMode="decimal" value={v.margem} onChange={(e) => set("margem", e.target.value)} placeholder="0,00" />
      </Fld>
      <Fld label="Observações" full>
        <textarea value={v.observacoes} onChange={(e) => set("observacoes", e.target.value)} />
      </Fld>
    </FormSheet>
  );
}
