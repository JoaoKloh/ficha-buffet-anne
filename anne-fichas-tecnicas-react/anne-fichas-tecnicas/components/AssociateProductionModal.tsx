"use client";

import { useEffect, useState } from "react";
import { Modal } from "./ui/Modal";
import { listarProducoesDisponiveis } from "@/lib/api/pratoProducoes";
import { AssociationPratoProducaoRequestSchema } from "@/lib/models/prato";
import type { AssociationPratoProducaoRequestDTO } from "@/lib/models/prato";
import type { ProducaoResponseDTO } from "@/lib/models/producao";
import { fmtDate } from "@/lib/utils/format";

interface Props {
  dish: { id: number; nome: string };
  onClose: () => void;
  onSave: (input: AssociationPratoProducaoRequestDTO) => Promise<void>;
}

export function AssociateProductionModal({ dish, onClose, onSave }: Props) {
  const [producoes, setProducoes] = useState<ProducaoResponseDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [errors, setErrors] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    listarProducoesDisponiveis()
      .then(setProducoes)
      .catch(() => setProducoes([]))
      .finally(() => setLoading(false));
  }, []);

  function toggle(id: number) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const payload: AssociationPratoProducaoRequestDTO = {
      pratoId: dish.id,
      eventosId: Array.from(selected),
    };
    const parsed = AssociationPratoProducaoRequestSchema.safeParse(payload);
    if (!parsed.success) {
      setErrors(parsed.error.issues.map((i) => i.message));
      return;
    }
    setErrors([]);
    setSaving(true);
    try {
      await onSave(parsed.data);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={`Associar "${dish.nome}" a eventos`} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {loading ? (
          <p className="dish-recipe-preview">Carregando eventos disponíveis...</p>
        ) : producoes.length === 0 ? (
          <p className="dish-recipe-preview">
            Nenhum evento/produção cadastrado ainda. Crie uma produção na aba &quot;Produção por
            evento&quot; para poder associá-la a este prato.
          </p>
        ) : (
          <div className="assoc-list">
            {producoes.map((p) => (
              <label key={p.id} className="assoc-row">
                <input
                  type="checkbox"
                  checked={selected.has(p.id)}
                  onChange={() => toggle(p.id)}
                />
                <span>
                  <span className="assoc-name">{p.nome}</span>
                  <span className="assoc-meta">
                    {fmtDate(p.data)}
                    {p.quantidade ? ` · ${p.quantidade} convidados` : ""}
                  </span>
                </span>
              </label>
            ))}
          </div>
        )}

        {errors.length > 0 && (
          <div className="error-text">
            {errors.map((err, i) => (
              <div key={i}>{err}</div>
            ))}
          </div>
        )}

        <div className="modal-actions">
          <button type="button" className="btn ghost" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button type="submit" className="btn" disabled={saving || selected.size === 0}>
            {saving ? "Salvando..." : "Salvar associação"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
