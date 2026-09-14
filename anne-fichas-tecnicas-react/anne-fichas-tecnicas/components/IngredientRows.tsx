"use client";

import type { Ingrediente } from "@/lib/models/dish";

interface Props {
  ingredientes: Ingrediente[];
  onChange: (next: Ingrediente[]) => void;
}

export function IngredientRows({ ingredientes, onChange }: Props) {
  function updateRow(index: number, patch: Partial<Ingrediente>) {
    const next = ingredientes.map((ing, i) => (i === index ? { ...ing, ...patch } : ing));
    onChange(next);
  }
  function removeRow(index: number) {
    onChange(ingredientes.filter((_, i) => i !== index));
  }
  function addRow() {
    onChange([...ingredientes, { nome: "", qtd: 0, unidade: "" }]);
  }

  return (
    <div>
      {ingredientes.map((ing, i) => (
        <div className="ing-row" key={i}>
          <input
            type="text"
            placeholder="Ingrediente"
            value={ing.nome}
            onChange={(e) => updateRow(i, { nome: e.target.value })}
          />
          <input
            type="number"
            min={0}
            step="0.01"
            placeholder="Qtd"
            value={ing.qtd || ""}
            onChange={(e) => updateRow(i, { qtd: Number(e.target.value) || 0 })}
          />
          <input
            type="text"
            placeholder="Unidade"
            value={ing.unidade}
            onChange={(e) => updateRow(i, { unidade: e.target.value })}
          />
          <button
            type="button"
            className="row-remove"
            title="Remover"
            onClick={() => removeRow(i)}
          >
            ✕
          </button>
        </div>
      ))}
      <button type="button" className="btn small ghost" onClick={addRow}>
        + Adicionar ingrediente
      </button>
    </div>
  );
}
