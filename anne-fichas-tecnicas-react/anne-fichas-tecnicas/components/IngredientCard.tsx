"use client";

import type { IngredienteResponseDTO } from "@/lib/models/ingredient";
import { fmtCurrency } from "@/lib/utils/format";

interface Props {
  ingredient: IngredienteResponseDTO;
  onEdit: (ingredient: IngredienteResponseDTO) => void;
  onDelete: (ingredient: IngredienteResponseDTO) => void;
}

export function IngredientCard({ ingredient, onEdit, onDelete }: Props) {
  return (
    <div className="dish-card">
      <div className="dish-body">
        <div className="dish-cat">{ingredient.categoria}</div>
        <div className="dish-name">{ingredient.nome}</div>
        <div className="dish-yield">
          {/* Custo/fornecedor são opcionais no cadastro — quando ausentes,
              mostramos o rótulo do campo sem valor em vez de escondê-lo. */}
          Unidade: {ingredient.unidade}
          <br />
          Custo: {ingredient.custo != null ? fmtCurrency(ingredient.custo) : ""}
          <br />
          Fornecedor: {ingredient.fornecedor ?? ""}
        </div>
        <div className="dish-actions">
          <button
            type="button"
            className="btn small ghost icon-btn"
            onClick={() => onEdit(ingredient)}
            aria-label={`Editar ${ingredient.nome}`}
            title="Editar ingrediente"
          >
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z" />
            </svg>
            Editar
          </button>
          <button type="button" className="btn small danger" onClick={() => onDelete(ingredient)}>
            Excluir
          </button>
        </div>
      </div>
    </div>
  );
}
