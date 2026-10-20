"use client";

import type { IngredienteResponseDTO } from "@/lib/models/ingredient";
import { IngredientCard } from "./IngredientCard";

interface Props {
  ingredients: IngredienteResponseDTO[];
  activeCategory: string;
  searchQuery?: string;
  onEdit: (ingredient: IngredienteResponseDTO) => void;
  onDelete: (ingredient: IngredienteResponseDTO) => void;
}

export function IngredientGrid({
  ingredients,
  activeCategory,
  searchQuery = "",
  onEdit,
  onDelete,
}: Props) {
  const byCategory =
    activeCategory === "Todos"
      ? ingredients
      : ingredients.filter((i) => i.categoria === activeCategory);
  const query = searchQuery.trim().toLowerCase();
  const list = query
    ? byCategory.filter((i) => i.nome.toLowerCase().includes(query))
    : byCategory;

  if (list.length === 0) {
    return (
      <div className="dish-grid">
        <div className="empty-state">
          {query
            ? "Nenhum ingrediente encontrado para essa busca."
            : `Nenhum ingrediente cadastrado ${activeCategory !== "Todos" ? "nesta categoria" : "ainda"}.`}
        </div>
      </div>
    );
  }

  return (
    <div className="dish-grid">
      {list.map((ingredient) => (
        <IngredientCard
          key={ingredient.id}
          ingredient={ingredient}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      ))}
    </div>
  );
}
