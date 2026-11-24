"use client";

import { useState } from "react";
import { Datebox, FormSheet, Fld, Sel, Vazio, msgErro } from "@/components/ui/Gestao";
import { useSheet } from "@/components/ui/Sheet";
import { useToast } from "@/components/ui/Toast";
import { TIPOS_OCORRENCIA, type TipoOcorrencia } from "@/lib/models/dominio";
import type { CozinhaProjecaoDTO } from "@/lib/models/cozinha";
import { criarOcorrencia, resolverOcorrencia } from "@/lib/api/gestao/cozinha";
import { useCozinha } from "./comum";
import { lerNome } from "./Ordens";

/** Ocorrências do evento: tudo que fugiu do planejado, no histórico do evento. */
export function AbaOcorrencias({ pj }: { pj: CozinhaProjecaoDTO }) {
  const { dados } = useCozinha();
  const { abrir, recarregar } = useSheet();
  const { showToast } = useToast();
  const oc = (dados.ocorrencias ?? []).filter((o) => o.eventoId === pj.eventoId).sort((a, b) => b.quando.localeCompare(a.quando));
  return (
    <>
      <div className="toolbar between">
        <p className="note" style={{ flex: "1 1 260px" }}>
          Registre tudo que fugiu do planejado: atraso, falta de insumo, temperatura, problema no carregamento, equipamento, acidente, pedido do cliente. Fica no histórico do evento.
        </p>
        <button type="button" className="btn primary" onClick={() => abrir("Registrar ocorrência", <OcorrenciaForm eventoId={pj.eventoId} />)}>
          Registrar ocorrência
        </button>
      </div>
      <section className="panel">
        <div className="panel-b">
          {oc.length ? (
            oc.map((o) => (
              <div key={o.id} className="row static" style={{ alignItems: "flex-start" }}>
                <Datebox iso={o.quando.slice(0, 10)} />
                <span className="grow">
                  <span className="t wrap">
                    {o.tipo}
                    {o.quem ? ` · ${o.quem}` : ""}
                  </span>
                  <span className="m wrap">{o.texto}</span>
                  {o.acao && <span className="m wrap">Ação: {o.acao}</span>}
                </span>
                <label className="check" style={{ fontSize: 13, fontWeight: 500 }}>
                  <input
                    type="checkbox"
                    checked={o.resolvida}
                    onChange={async (e) => {
                      try {
                        await resolverOcorrencia(o.id, e.target.checked);
                        recarregar();
                      } catch (err) {
                        showToast(msgErro(err), "error");
                      }
                    }}
                  />{" "}
                  resolvida
                </label>
              </div>
            ))
          ) : (
            <Vazio titulo="Nenhuma ocorrência">Tudo dentro do planejado até agora.</Vazio>
          )}
        </div>
      </section>
    </>
  );
}

function OcorrenciaForm({ eventoId }: { eventoId: number }) {
  const [v, setV] = useState({ tipo: "Atraso" as TipoOcorrencia, quem: lerNome(), texto: "", acao: "" });
  return (
    <FormSheet
      validar={() => (!v.quem.trim() ? "Informe quem registra." : !v.texto.trim() ? "Descreva o que aconteceu." : null)}
      onSalvar={() => criarOcorrencia({ eventoId, tipo: v.tipo, quem: v.quem.trim(), texto: v.texto.trim(), acao: v.acao.trim() })}
      okMsg="Ocorrência registrada"
    >
      <Fld label="Tipo">
        <Sel value={v.tipo} onChange={(x) => setV({ ...v, tipo: x as TipoOcorrencia })} opcoes={TIPOS_OCORRENCIA.map((t) => [t, t] as const)} />
      </Fld>
      <Fld label="Quem registra" obrig>
        <input value={v.quem} onChange={(e) => setV({ ...v, quem: e.target.value })} />
      </Fld>
      <Fld label="O que aconteceu" obrig full>
        <textarea value={v.texto} onChange={(e) => setV({ ...v, texto: e.target.value })} autoFocus />
      </Fld>
      <Fld label="O que foi feito" full>
        <input value={v.acao} onChange={(e) => setV({ ...v, acao: e.target.value })} />
      </Fld>
    </FormSheet>
  );
}
