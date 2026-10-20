"use client";

import { useState } from "react";
import { Modal } from "./ui/Modal";
import type { OrcamentoSalvo } from "@/lib/models/orcamento";
import { fmtCurrency } from "@/lib/utils/format";

export function SalvarOrcamentoModal({
  nomeInicial,
  onSalvar,
  onClose,
}: {
  nomeInicial: string;
  onSalvar: (nome: string) => Promise<void>;
  onClose: () => void;
}) {
  const [nome, setNome] = useState(nomeInicial);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await onSalvar(nome.trim() || "Orçamento sem nome");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Salvar orçamento" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div className="field-block">
          <label htmlFor="orc-salvar-nome">Nome do orçamento</label>
          <input
            id="orc-salvar-nome"
            type="text"
            autoFocus
            value={nome}
            onChange={(e) => setNome(e.target.value)}
          />
        </div>
        <div className="modal-actions">
          <button type="button" className="btn ghost" onClick={onClose}>
            Cancelar
          </button>
          <button type="submit" className="btn" disabled={saving}>
            {saving ? "Salvando..." : "Salvar"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

const SEM_DETALHES = "Salvo em outro navegador: só os totais estão disponíveis aqui.";

export function MeusOrcamentosModal({
  orcamentos,
  onAbrir,
  onCompartilhar,
  onExcluir,
  onClose,
}: {
  orcamentos: OrcamentoSalvo[];
  onAbrir: (o: OrcamentoSalvo) => void;
  onCompartilhar: (o: OrcamentoSalvo) => void;
  onExcluir: (o: OrcamentoSalvo) => void;
  onClose: () => void;
}) {
  return (
    <Modal title="Meus orçamentos" onClose={onClose}>
      {orcamentos.length === 0 ? (
        <p className="fin-muted orc-empty">
          Nenhum orçamento salvo ainda. Use &quot;Salvar&quot; para guardar este orçamento.
        </p>
      ) : (
        <ul className="orc-salvos">
          {orcamentos.map((o) => {
            const nomeEvento = o.orcamento?.evento.nome;
            return (
              <li key={o.id} className="orc-salvo">
                <div className="orc-salvo-body">
                  <div className="assoc-name">{nomeEvento || o.nomeCliente}</div>
                  <div className="assoc-meta">
                    {[nomeEvento ? o.nomeCliente : "", o.tipoServico, `${o.numeroConvidados} pax`, fmtCurrency(o.valorTotal)]
                      .filter(Boolean)
                      .join(" · ")}
                  </div>
                </div>
                <button
                  type="button"
                  className="btn ghost small"
                  onClick={() => onAbrir(o)}
                  disabled={!o.orcamento}
                  title={o.orcamento ? undefined : SEM_DETALHES}
                >
                  Abrir
                </button>
                <button
                  type="button"
                  className="btn ghost small"
                  onClick={() => onCompartilhar(o)}
                  disabled={!o.orcamento}
                  title={o.orcamento ? undefined : SEM_DETALHES}
                >
                  Compartilhar
                </button>
                <button type="button" className="btn danger small" onClick={() => onExcluir(o)}>
                  Excluir
                </button>
              </li>
            );
          })}
        </ul>
      )}
      <div className="modal-actions">
        <button type="button" className="btn ghost" onClick={onClose}>
          Fechar
        </button>
      </div>
    </Modal>
  );
}
