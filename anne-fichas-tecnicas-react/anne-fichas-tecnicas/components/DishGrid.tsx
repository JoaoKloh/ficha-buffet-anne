"use client";

import type { Categoria } from "@/lib/models/category";
import type { Dish } from "@/lib/models/dish";
import { DishCard } from "./DishCard";

interface Props {
  dishes: Dish[];
  activeCategory: Categoria | "Todos";
  onEdit: (dish: Dish) => void;
  onDelete: (dish: Dish) => void;
}

export function DishGrid({ dishes, activeCategory, onEdit, onDelete }: Props) {
  const list =
    activeCategory === "Todos" ? dishes : dishes.filter((d) => d.categoria === activeCategory);

  if (list.length === 0) {
    return (
      <div className="dish-grid">
        <div className="empty-state">
          Nenhum prato cadastrado {activeCategory !== "Todos" ? "nesta categoria" : "ainda"}.
        </div>
      </div>
    );
  }

  return (
    <div className="dish-grid">
      {list.map((dish) => (
        <DishCard key={dish.id} dish={dish} onEdit={onEdit} onDelete={onDelete} />
      ))}
    </div>
  );
}
