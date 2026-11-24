"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Header } from "@/components/Header";
import { Carregando, Chips, Vazio } from "@/components/ui/Gestao";
import { useDados, useSheet } from "@/components/ui/Sheet";
import { FILTROS_EVENTO, type FiltroEvento } from "@/lib/models/evento";
import { listarEventos } from "@/lib/api/gestao/eventos";
import { capitalizar, ymLabel } from "@/lib/utils/numero";
import { EvRow } from "./EvRow";
import { EventoForm } from "./EventoForm";
import { EventoSheet } from "./EventoSheet";

const FILTRO_KEY = "anne:ev-filtro";

export function EventosView() {
  const { abrir } = useSheet();
  const sp = useSearchParams();
  const [filtro, setFiltro] = useState<FiltroEvento>("ativos");
  const [busca, setBusca] = useState("");
  const { dados, erro } = useDados(() => listarEventos(filtro, busca), [filtro, busca]);

  useEffect(() => {
    try {
      const s = window.localStorage.getItem(FILTRO_KEY);
      if (s && FILTROS_EVENTO.some((f) => f[0] === s)) {
        setFiltro(s as FiltroEvento);
      }
    } catch {
      // ignora restrições de armazenamento
    }
  }, []);

  // Atalhos do Painel e de outras telas: /eventos?abrir=ID abre a ficha; ?novo=1 abre o formulário.
  const abrirId = sp.get("abrir");
  const novo = sp.get("novo");
  useEffect(() => {
    if (abrirId) abrir("Evento", <EventoSheet id={Number(abrirId)} />);
    else if (novo) abrir("Novo evento", <EventoForm />);
  }, [abrirId, novo, abrir]);

  function trocar(f: FiltroEvento) {
    setFiltro(f);
    try {
      window.localStorage.setItem(FILTRO_KEY, f);
    } catch {
      // preferência de tela: sem armazenamento, fica só nesta visita
    }
  }

  return (
    <div className="app">
      <Header
        sub={dados ? `${dados.ativos} ativos` : undefined}
        actions={
          <button type="button" className="btn primary" onClick={() => abrir("Novo evento", <EventoForm />)}>
            Novo evento
          </button>
        }
      />
      <Chips opcoes={FILTROS_EVENTO} valor={filtro} onChange={trocar} rotulo="Filtro de eventos" />
      <div className="toolbar">
        <input className="search" type="search" placeholder="Buscar por código, nome, cliente ou local" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar evento" />
      </div>
      <section className="panel">
        <div className="panel-b">
          {!dados ? (
            <Carregando erro={erro} />
          ) : !dados.eventos.length ? (
            dados.total ? (
              <Vazio titulo="Nenhum evento neste filtro">Troque o filtro ou a busca.</Vazio>
            ) : (
              <Vazio titulo="Nenhum evento cadastrado">Comece pelo pedido de orçamento: Novo evento cria o evento como lead.</Vazio>
            )
          ) : (
            dados.eventos.map((e, i) => {
              const m = (e.dataEvento || "").slice(0, 7);
              const cab = i === 0 || (dados.eventos[i - 1]!.dataEvento || "").slice(0, 7) !== m ? <div className="monthhead">{m ? capitalizar(ymLabel(m)) : "Sem data"}</div> : null;
              return (
                <div key={e.id}>
                  {cab}
                  <EvRow e={e} onClick={() => abrir(e.nome, <EventoSheet id={e.id} />)} />
                </div>
              );
            })
          )}
        </div>
      </section>
    </div>
  );
}