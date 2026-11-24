"use client";

import { Datebox, Pill } from "@/components/ui/Gestao";
import type { EventoListaItemDTO } from "@/lib/models/evento";
import { stLabel, stTone, tipoLabel } from "./status";

/** Linha de evento (lista de Eventos e próximos eventos do Painel). */
export function EvRow({ e, onClick }: { e: EventoListaItemDTO; onClick: () => void }) {
  const meta = [tipoLabel(e.tipo), e.local || e.cidade, e.convidadosPrevistos ? `${e.convidadosPrevistos} convidados` : null].filter(Boolean).join(" · ");
  const tn = stTone(e.status);
  return (
    <button type="button" className={`row${tn === "deg" || tn === "fech" ? ` st-${tn}` : ""}`} onClick={onClick}>
      <Datebox iso={e.dataEvento} />
      <span className="grow">
        <span className="t">
          <span className="num" style={{ fontWeight: 700 }}>
            {e.codigo}
          </span>{" "}
          · {e.nome}
        </span>
        <span className="m">{meta || e.clienteNome || ""}</span>
      </span>
      <Pill tom={tn}>{stLabel(e.status)}</Pill>
    </button>
  );
}
