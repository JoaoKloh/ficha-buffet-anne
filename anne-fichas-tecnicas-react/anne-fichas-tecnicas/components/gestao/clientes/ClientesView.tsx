"use client";

import { useState } from "react";
import { Header } from "@/components/Header";
import { Carregando, Vazio } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { listarClientes } from "@/lib/api/gestao/clientes";
import { ORIGENS_CLIENTE, rotulo } from "@/lib/models/dominio";
import { fdate } from "@/lib/utils/numero";
import { ClienteForm } from "./ClienteForm";

export function ClientesView() {
  const [busca, setBusca] = useState("");
  const { abrir } = useSheet();
  const { dados, erro } = useDados(() => listarClientes(busca), [busca]);
  const total = dados?.length ?? 0;

  return (
    <div className="app">
      <Header
        sub={busca ? `${total} encontrado${total === 1 ? "" : "s"}` : `${total} cadastrado${total === 1 ? "" : "s"}`}
        actions={
          <button type="button" className="btn primary" onClick={() => abrir("Novo cliente", <ClienteForm />)}>
            Novo cliente
          </button>
        }
      />
      <div className="toolbar">
        <input className="search" type="search" placeholder="Buscar por nome, telefone ou e-mail" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar cliente" />
      </div>
      <section className="panel">
        <div className="panel-b">
          {!dados ? (
            <Carregando erro={erro} />
          ) : !dados.length ? (
            busca ? (
              <Vazio titulo="Nenhum cliente encontrado">Tente outro termo.</Vazio>
            ) : (
              <Vazio titulo="Nenhum cliente cadastrado">Clientes também são criados ao cadastrar um evento.</Vazio>
            )
          ) : (
            dados.map((c) => (
              <button
                key={c.id}
                type="button"
                className="row"
                onClick={() => abrir("Editar cliente", <ClienteForm cliente={c} temEventos={c.totalEventos > 0} />)}
              >
                <span className="grow">
                  <span className="t">{c.nome}</span>
                  <span className="m">{[c.telefone, c.email, c.origem ? rotulo(ORIGENS_CLIENTE, c.origem) : ""].filter(Boolean).join(" · ") || "sem contato"}</span>
                </span>
                <span className="m">
                  {c.totalEventos} evento{c.totalEventos === 1 ? "" : "s"}
                  {c.proximoEvento ? ` · próximo ${fdate(c.proximoEvento)}` : ""}
                </span>
              </button>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
