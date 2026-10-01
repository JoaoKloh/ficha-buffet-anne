"use client";

import { useState } from "react";
import { Modal } from "./ui/Modal";
import { CATEGORIAS_INGREDIENTE } from "@/lib/models/ingredient";
import type { IngredienteResponseDTO, CreateIngredienteRequestDTO } from "@/lib/models/ingredient";
import { CreateIngredienteRequestSchema } from "@/lib/models/ingredient";

interface Props {
  initial: IngredienteResponseDTO | null;
  onClose: () => void;
  onSave: (input: CreateIngredienteRequestDTO) => Promise<void>;
}

const emptyForm: CreateIngredienteRequestDTO = {
  nome: "",
  categoria: CATEGORIAS_INGREDIENTE[0],
  unidade: "kg",
  custo: 0,
  fornecedor: "",
  descricao: "",
};

export function IngredientFormModal({ initial, onClose, onSave }: Props) {
  const [form, setForm] = useState<CreateIngredienteRequestDTO>(
    initial
      ? {
          nome: initial.nome,
          categoria: initial.categoria,
          unidade: initial.unidade,
          custo: initial.custo ?? 0,
          fornecedor: initial.fornecedor ?? "",
          // O backend não devolve a descrição ao listar (ver IngredienteResponseDTO
          // em lib/models/ingredient.ts) — não há como pré-preencher; editar
          // sempre parte de uma descrição em branco.
          descricao: "",
        }
      : emptyForm
  );
  const [errors, setErrors] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const parsed = CreateIngredienteRequestSchema.safeParse(form);
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
    <Modal title={initial ? "Editar ingrediente" : "Novo ingrediente"} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div className="row2">
          <div className="field-block">
            <label htmlFor="i_nome">Nome do ingrediente</label>
            <input
              id="i_nome"
              type="text"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              placeholder="Ex: Filé mignon"
            />
          </div>
          <div className="field-block">
            <label htmlFor="i_categoria">Categoria</label>
            <select
              id="i_categoria"
              value={form.categoria}
              onChange={(e) => setForm({ ...form, categoria: e.target.value })}
            >
              {CATEGORIAS_INGREDIENTE.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="row2">
          <div className="field-block">
            <label htmlFor="i_unidade">Unidade de medida</label>
            <input
              id="i_unidade"
              type="text"
              value={form.unidade}
              onChange={(e) => setForm({ ...form, unidade: e.target.value })}
              placeholder="Ex: kg, L, un"
            />
          </div>
          <div className="field-block">
            <label htmlFor="i_custo">Custo por unidade (R$)</label>
            <input
              id="i_custo"
              type="number"
              min={0}
              step="0.01"
              value={form.custo}
              onChange={(e) => setForm({ ...form, custo: Number(e.target.value) || 0 })}
            />
          </div>
        </div>

        <div className="field-block">
          <label htmlFor="i_fornecedor">Fornecedor (opcional)</label>
          <input
            id="i_fornecedor"
            type="text"
            value={form.fornecedor}
            onChange={(e) => setForm({ ...form, fornecedor: e.target.value })}
            placeholder="Ex: Distribuidora Central"
          />
        </div>

        <div className="field-block">
          <label htmlFor="i_descricao">Descrição</label>
          <textarea
            id="i_descricao"
            value={form.descricao}
            onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            placeholder="Notas sobre armazenamento, validade, marca preferida..."
          />
        </div>

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
            {saving ? "Salvando..." : "Salvar ingrediente"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
