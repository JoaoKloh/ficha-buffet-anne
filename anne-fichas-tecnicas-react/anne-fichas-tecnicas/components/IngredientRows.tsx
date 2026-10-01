"use client";

import { useEffect, useState } from "react";
import type { Ingrediente } from "@/lib/models/dish";
import { listarIngredientesDisponiveis } from "@/lib/api/pratoIngredientes";
import type { IngredienteResponseDTO } from "@/lib/models/prato";

interface Props {
  ingredientes: Ingrediente[];
  onChange: (next: Ingrediente[]) => void;
}

export function IngredientRows({ ingredientes, onChange }: Props) {
  const [catalogo, setCatalogo] = useState<IngredienteResponseDTO[]>([]);

  useEffect(() => {
    listarIngredientesDisponiveis()
      .then(setCatalogo)
      .catch(() => setCatalogo([]));
  }, []);

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

  // Selecionar no menu suspenso grava o id real do ingrediente (enviado ao
  // backend como ingredienteId) e usa nome/unidade do catálogo para exibição.
  function handleSelect(index: number, idSelecionado: string) {
    const selecionado = catalogo.find((c) => String(c.id) === idSelecionado);
    if (!selecionado) {
      updateRow(index, { ingredienteId: undefined, nome: "", unidade: "" });
      return;
    }
    updateRow(index, {
      ingredienteId: selecionado.id,
      nome: selecionado.nome,
      unidade: selecionado.unidade,
    });
  }

  return (
    <div>
      {ingredientes.map((ing, i) => (
        <div className="ing-row" key={i}>
          <select value={ing.ingredienteId ?? ""} onChange={(e) => handleSelect(i, e.target.value)}>
            <option value="">Selecione um ingrediente</option>
            {catalogo.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}
              </option>
            ))}
          </select>
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
