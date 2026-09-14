"use client";

import { useState } from "react";
import { Modal } from "./ui/Modal";
import { IngredientRows } from "./IngredientRows";
import { CATEGORIAS } from "@/lib/models/category";
import type { Dish, CreateDishInput } from "@/lib/models/dish";
import { CreateDishInputSchema } from "@/lib/models/dish";

interface Props {
  initial: Dish | null;
  onClose: () => void;
  onSave: (input: CreateDishInput) => Promise<void>;
}

const emptyForm: CreateDishInput = {
  nome: "",
  categoria: CATEGORIAS[0],
  foto: "",
  rendQtd: 1,
  rendUnid: "unidades",
  porPessoa: 0,
  receita: "",
  ingredientes: [],
};

export function DishFormModal({ initial, onClose, onSave }: Props) {
  const [form, setForm] = useState<CreateDishInput>(
    initial
      ? {
          nome: initial.nome,
          categoria: initial.categoria,
          foto: initial.foto ?? "",
          rendQtd: initial.rendQtd,
          rendUnid: initial.rendUnid,
          porPessoa: initial.porPessoa,
          receita: initial.receita,
          ingredientes: initial.ingredientes,
        }
      : emptyForm
  );
  const [errors, setErrors] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const parsed = CreateDishInputSchema.safeParse(form);
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
    <Modal title={initial ? "Editar prato" : "Novo prato"} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div className="row2">
          <div className="field-block">
            <label htmlFor="d_nome">Nome do prato</label>
            <input
              id="d_nome"
              type="text"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              placeholder="Ex: Brie Folhado"
            />
          </div>
          <div className="field-block">
            <label htmlFor="d_categoria">Categoria</label>
            <select
              id="d_categoria"
              value={form.categoria}
              onChange={(e) => setForm({ ...form, categoria: e.target.value as Dish["categoria"] })}
            >
              {CATEGORIAS.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="field-block">
          <label htmlFor="d_foto">URL da foto (https, opcional)</label>
          <input
            id="d_foto"
            type="text"
            value={form.foto}
            onChange={(e) => setForm({ ...form, foto: e.target.value })}
            placeholder="https://..."
          />
        </div>

        <div className="row2">
          <div className="field-block">
            <label htmlFor="d_rendQtd">Rendimento da receita (qtd.)</label>
            <input
              id="d_rendQtd"
              type="number"
              min={1}
              step="1"
              value={form.rendQtd}
              onChange={(e) => setForm({ ...form, rendQtd: Number(e.target.value) || 1 })}
            />
          </div>
          <div className="field-block">
            <label htmlFor="d_rendUnid">Unidade do rendimento</label>
            <input
              id="d_rendUnid"
              type="text"
              value={form.rendUnid}
              onChange={(e) => setForm({ ...form, rendUnid: e.target.value })}
              placeholder="Ex: unidades, g, bandejas"
            />
          </div>
        </div>

        <div className="field-block">
          <label htmlFor="d_porPessoa">Sugestão por pessoa</label>
          <input
            id="d_porPessoa"
            type="number"
            min={0}
            step="0.1"
            value={form.porPessoa}
            onChange={(e) => setForm({ ...form, porPessoa: Number(e.target.value) || 0 })}
          />
        </div>

        <div className="field-block">
          <label htmlFor="d_receita">Modo de preparo</label>
          <textarea
            id="d_receita"
            value={form.receita}
            onChange={(e) => setForm({ ...form, receita: e.target.value })}
            placeholder="Passo a passo da receita..."
          />
        </div>

        <div className="section-label">Ingredientes (para o rendimento acima)</div>
        <IngredientRows
          ingredientes={form.ingredientes}
          onChange={(ingredientes) => setForm({ ...form, ingredientes })}
        />

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
            {saving ? "Salvando..." : "Salvar prato"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
