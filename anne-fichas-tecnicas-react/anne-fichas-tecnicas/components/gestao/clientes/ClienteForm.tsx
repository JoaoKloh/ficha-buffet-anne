"use client";

import { useState } from "react";
import { FormSheet, Fld, Sel } from "@/components/ui/Gestao";
import { ORIGENS_CLIENTE, TIPOS_CLIENTE, type OrigemCliente, type TipoCliente } from "@/lib/models/dominio";
import type { ClienteResponseDTO } from "@/lib/models/cliente";
import { atualizarCliente, criarCliente, excluirCliente } from "@/lib/api/gestao/clientes";

/** Novo ou editar cliente. Excluir só aparece para cliente sem eventos. */
export function ClienteForm({ cliente, temEventos, onSalvo }: { cliente?: ClienteResponseDTO; temEventos?: boolean; onSalvo?: (c: ClienteResponseDTO) => void }) {
  const [v, setV] = useState({
    nome: cliente?.nome ?? "",
    tipo: (cliente?.tipo ?? "PF") as TipoCliente,
    documento: cliente?.documento ?? "",
    telefone: cliente?.telefone ?? "",
    email: cliente?.email ?? "",
    origem: (cliente?.origem ?? "") as OrigemCliente | "",
  });
  const set = <K extends keyof typeof v>(k: K, x: (typeof v)[K]) => setV((s) => ({ ...s, [k]: x }));

  return (
    <FormSheet
      validar={() => (v.nome.trim() ? null : "Informe o nome do cliente.")}
      onSalvar={() => (cliente ? atualizarCliente({ ...v, id: cliente.id }) : criarCliente(v))}
      depois={onSalvo ? (c) => onSalvo(c as ClienteResponseDTO) : undefined}
      onExcluir={cliente && !temEventos ? () => excluirCliente(cliente.id) : null}
      excluirConfirma="Excluir este cliente?"
    >
      <Fld label="Nome" obrig full>
        <input value={v.nome} onChange={(e) => set("nome", e.target.value)} autoFocus />
      </Fld>
      <Fld label="Tipo">
        <Sel value={v.tipo} onChange={(x) => set("tipo", x as TipoCliente)} opcoes={TIPOS_CLIENTE} />
      </Fld>
      <Fld label="CPF ou CNPJ">
        <input value={v.documento} onChange={(e) => set("documento", e.target.value)} />
      </Fld>
      <Fld label="Telefone / WhatsApp">
        <input type="tel" value={v.telefone} onChange={(e) => set("telefone", e.target.value)} />
      </Fld>
      <Fld label="E-mail">
        <input type="email" value={v.email} onChange={(e) => set("email", e.target.value)} />
      </Fld>
      <Fld label="Como chegou">
        <Sel value={v.origem} onChange={(x) => set("origem", x as OrigemCliente | "")} opcoes={ORIGENS_CLIENTE} vazio="—" />
      </Fld>
    </FormSheet>
  );
}
