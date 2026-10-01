"use client";

import { useEffect, useState } from "react";
import { Modal } from "./ui/Modal";
import { listarPratosDisponiveis } from "@/lib/api/producaoPratos";
import { CreateProducaoRequestSchema } from "@/lib/models/producao";
import type { CreateProducaoRequestDTO, ProducaoResponseDTO } from "@/lib/models/producao";
import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";

interface Props {
  initial: ProducaoResponseDTO | null;
  onClose: () => void;
  onSave: (input: CreateProducaoRequestDTO) => Promise<void>;
}

function toDraftPratoIds(producao: ProducaoResponseDTO | null): string[] {
  if (!producao || producao.pratos.length === 0) return [""];
  return producao.pratos.map((p) => String(p.id));
}

export function ProductionFormModal({ initial, onClose, onSave }: Props) {
  const [nome, setNome] = useState(initial?.nome ?? "");
  const [data, setData] = useState(initial?.data ?? "");
  const [quantidade, setQuantidade] = useState(initial?.quantidade ?? 0);
  const [pratoIds, setPratoIds] = useState<string[]>(toDraftPratoIds(initial));
  const [errors, setErrors] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  const [catalogo, setCatalogo] = useState<PratoDetalhadoResponseDTO[]>([]);

  useEffect(() => {
    listarPratosDisponiveis()
      .then(setCatalogo)
      .catch(() => setCatalogo([]));
  }, []);

  function updateRow(index: number, pratoId: string) {
    setPratoIds((prev) => prev.map((v, i) => (i === index ? pratoId : v)));
  }
  function removeRow(index: number) {
    setPratoIds((prev) => prev.filter((_, i) => i !== index));
  }
  function addRow() {
    setPratoIds((prev) => [...prev, ""]);
  }
  // Cada linha só mostra pratos ainda não escolhidos nas outras linhas —
  // o backend rejeita o mesmo pratoId repetido numa mesma produção.
  function disponiveisPara(index: number): PratoDetalhadoResponseDTO[] {
    const escolhidosAlhures = new Set(pratoIds.filter((_, i) => i !== index).filter(Boolean));
    return catalogo.filter((c) => !escolhidosAlhures.has(String(c.id)));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const payload: CreateProducaoRequestDTO = {
      nome,
      quantidade,
      data,
      pratos: pratoIds
        .filter((id) => id !== "")
        .map((id) => ({ pratoId: Number(id) })) as CreateProducaoRequestDTO["pratos"],
    };
    const parsed = CreateProducaoRequestSchema.safeParse(payload);
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
    <Modal title={initial ? `Editar produção #${initial.id}` : "Nova produção"} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div className="row2">
          <div className="field-block">
            <label htmlFor="pr_nome">Nome do evento</label>
            <input
              id="pr_nome"
              type="text"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              placeholder="Ex: Marina & Rafael"
            />
          </div>
          <div className="field-block">
            <label htmlFor="pr_data">Data do evento</label>
            <input id="pr_data" type="date" value={data} onChange={(e) => setData(e.target.value)} />
          </div>
        </div>

        <div className="field-block">
          <label htmlFor="pr_quantidade">Nº de convidados</label>
          <input
            id="pr_quantidade"
            type="number"
            min={1}
            step="1"
            value={quantidade || ""}
            onChange={(e) => setQuantidade(Number(e.target.value) || 0)}
            placeholder="Ex: 150"
          />
        </div>

        <div className="section-label">Pratos da produção</div>
        {pratoIds.map((pratoId, i) => (
          <div
            key={i}
            style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: 8, marginBottom: 8 }}
          >
            <select value={pratoId} onChange={(e) => updateRow(i, e.target.value)}>
              <option value="">Selecione um prato</option>
              {disponiveisPara(i).map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nome}
                </option>
              ))}
            </select>
            <button type="button" className="row-remove" title="Remover prato" onClick={() => removeRow(i)}>
              ✕
            </button>
          </div>
        ))}
        <button type="button" className="btn small ghost" onClick={addRow}>
          + Adicionar prato
        </button>
        {catalogo.length === 0 && (
          <p className="dish-recipe-preview" style={{ marginTop: 4 }}>
            Nenhum prato cadastrado no catálogo ainda.
          </p>
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
          <button type="submit" className="btn" disabled={saving}>
            {saving ? "Salvando..." : "Salvar produção"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
