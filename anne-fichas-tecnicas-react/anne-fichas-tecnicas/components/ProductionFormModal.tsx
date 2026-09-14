"use client";

import { useState } from "react";
import { Modal } from "./ui/Modal";
import type { Dish } from "@/lib/models/dish";
import type { Production, CreateProductionInput, ProductionItem } from "@/lib/models/production";
import { CreateProductionInputSchema } from "@/lib/models/production";

interface Props {
  initial: Production | null;
  dishes: Dish[];
  onClose: () => void;
  onSave: (input: CreateProductionInput) => Promise<void>;
}

interface DraftItem {
  dishId: string;
  quantidade: string;
}

function toDraftItems(itens: ProductionItem[]): DraftItem[] {
  if (itens.length === 0) return [{ dishId: "", quantidade: "" }];
  return itens.map((i) => ({ dishId: i.dishId, quantidade: String(i.quantidade) }));
}

export function ProductionFormModal({ initial, dishes, onClose, onSave }: Props) {
  const [evento, setEvento] = useState(initial?.evento ?? "");
  const [data, setData] = useState(initial?.data ?? "");
  const [convidados, setConvidados] = useState(initial?.convidados ?? 0);
  const [items, setItems] = useState<DraftItem[]>(toDraftItems(initial?.itens ?? []));
  const [errors, setErrors] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  const dishesByCategory = new Map<string, Dish[]>();
  for (const d of dishes) {
    const list = dishesByCategory.get(d.categoria) ?? [];
    list.push(d);
    dishesByCategory.set(d.categoria, list);
  }

  function updateItem(index: number, patch: Partial<DraftItem>) {
    setItems((prev) => prev.map((it, i) => (i === index ? { ...it, ...patch } : it)));
  }
  function removeItem(index: number) {
    setItems((prev) => prev.filter((_, i) => i !== index));
  }
  function addItem() {
    setItems((prev) => [...prev, { dishId: "", quantidade: "" }]);
  }
  function suggestForRow(index: number) {
    const row = items[index];
    if (!row) return;
    const dish = dishes.find((d) => d.id === row.dishId);
    if (!dish || !dish.porPessoa || !convidados) {
      setErrors(["Selecione um prato com sugestão por pessoa e informe o nº de convidados."]);
      return;
    }
    updateItem(index, { quantidade: String(Math.ceil(convidados * dish.porPessoa)) });
  }
  function recalcAll() {
    if (!convidados) {
      setErrors(["Informe o nº de convidados primeiro."]);
      return;
    }
    setItems((prev) =>
      prev.map((row) => {
        const dish = dishes.find((d) => d.id === row.dishId);
        if (dish && dish.porPessoa) {
          return { ...row, quantidade: String(Math.ceil(convidados * dish.porPessoa)) };
        }
        return row;
      })
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const itens = items
      .filter((it) => it.dishId && Number(it.quantidade) > 0)
      .map((it) => ({ dishId: it.dishId, quantidade: Number(it.quantidade) }));

    const payload: CreateProductionInput = { evento, data, convidados, itens };
    const parsed = CreateProductionInputSchema.safeParse(payload);
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
    <Modal title={initial ? "Editar produção" : "Nova produção"} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div className="row2">
          <div className="field-block">
            <label htmlFor="pr_evento">Nome do evento</label>
            <input
              id="pr_evento"
              type="text"
              value={evento}
              onChange={(e) => setEvento(e.target.value)}
              placeholder="Ex: Marina & Rafael"
            />
          </div>
          <div className="field-block">
            <label htmlFor="pr_data">Data do evento</label>
            <input id="pr_data" type="date" value={data} onChange={(e) => setData(e.target.value)} />
          </div>
        </div>
        <div className="field-block">
          <label htmlFor="pr_convidados">Nº de convidados</label>
          <input
            id="pr_convidados"
            type="number"
            min={0}
            step="1"
            value={convidados || ""}
            onChange={(e) => setConvidados(Number(e.target.value) || 0)}
            placeholder="Ex: 150"
          />
        </div>

        <div className="section-label">Pratos e quantidades a produzir</div>
        <div style={{ marginBottom: 10 }}>
          <button type="button" className="btn small ghost" onClick={recalcAll}>
            Calcular quantidades pelos convidados
          </button>
        </div>

        {items.map((row, i) => (
          <div
            key={i}
            style={{ display: "grid", gridTemplateColumns: "2fr 1fr auto auto", gap: 8, marginBottom: 8 }}
          >
            <select value={row.dishId} onChange={(e) => updateItem(i, { dishId: e.target.value })}>
              <option value="">Selecione um prato</option>
              {Array.from(dishesByCategory.entries()).map(([cat, list]) => (
                <optgroup label={cat} key={cat}>
                  {list.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.nome}
                    </option>
                  ))}
                </optgroup>
              ))}
            </select>
            <input
              type="number"
              min={0}
              step="1"
              placeholder="Qtd a produzir"
              value={row.quantidade}
              onChange={(e) => updateItem(i, { quantidade: e.target.value })}
            />
            <button type="button" className="btn small ghost" onClick={() => suggestForRow(i)}>
              Sugerir
            </button>
            <button type="button" className="row-remove" onClick={() => removeItem(i)}>
              ✕
            </button>
          </div>
        ))}
        <button type="button" className="btn small ghost" onClick={addItem}>
          + Adicionar prato
        </button>

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
