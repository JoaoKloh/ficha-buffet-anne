"use client";

import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";
import { DishCard } from "./DishCard";

interface Props {
  dishes: PratoDetalhadoResponseDTO[];
  activeCategory: string;
  searchQuery?: string;
  onEdit: (dish: PratoDetalhadoResponseDTO) => void;
  onDelete: (dish: PratoDetalhadoResponseDTO) => void;
  onAssociateProduction: (dish: PratoDetalhadoResponseDTO) => void;
}

export function DishGrid({
  dishes,
  activeCategory,
  searchQuery = "",
  onEdit,
  onDelete,
  onAssociateProduction,
}: Props) {
  const byCategory =
    activeCategory === "Todos" ? dishes : dishes.filter((d) => d.categoria === activeCategory);
  const query = searchQuery.trim().toLowerCase();
  const list = query
    ? byCategory.filter((d) => d.nome.toLowerCase().includes(query))
    : byCategory;

  if (list.length === 0) {
    return (
      <div className="dish-grid">
        <div className="empty-state">
          {query
            ? "Nenhum prato encontrado para essa busca."
            : `Nenhum prato cadastrado ${activeCategory !== "Todos" ? "nesta categoria" : "ainda"}.`}
        </div>
      </div>
    );
  }

  return (
    <div className="dish-grid">
      {list.map((dish) => (
        <DishCard
          key={dish.id}
          dish={dish}
          onEdit={onEdit}
          onDelete={onDelete}
          onAssociateProduction={onAssociateProduction}
        />
      ))}
    </div>
  );
}
