"use client";

import { useState } from "react";
import { Header } from "./Header";
import { CategoryChips } from "./CategoryChips";
import { DishGrid } from "./DishGrid";
import { DishFormModal } from "./DishFormModal";
import { useToast } from "./ui/Toast";
import { dishesApi } from "@/lib/api/dishes";
import { ApiError } from "@/lib/api/client";
import type { Dish, CreateDishInput } from "@/lib/models/dish";
import type { Categoria } from "@/lib/models/category";

interface Props {
  initialDishes: Dish[];
}

export function CatalogView({ initialDishes }: Props) {
  const [dishes, setDishes] = useState<Dish[]>(initialDishes);
  const [activeCategory, setActiveCategory] = useState<Categoria | "Todos">("Todos");
  const [editingDish, setEditingDish] = useState<Dish | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const { showToast } = useToast();

  function openNew() {
    setEditingDish(null);
    setModalOpen(true);
  }
  function openEdit(dish: Dish) {
    setEditingDish(dish);
    setModalOpen(true);
  }

  async function handleSave(input: CreateDishInput) {
    try {
      if (editingDish) {
        const updated = await dishesApi.update(editingDish.id, input);
        setDishes((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
        showToast("Prato atualizado.");
      } else {
        const created = await dishesApi.create(input);
        setDishes((prev) => [...prev, created]);
        showToast("Prato adicionado ao catálogo.");
      }
      setModalOpen(false);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "Não foi possível salvar o prato.";
      showToast(message, "error");
    }
  }

  async function handleDelete(dish: Dish) {
    if (!confirm(`Excluir "${dish.nome}" do catálogo? Essa ação não pode ser desfeita.`)) return;
    try {
      await dishesApi.remove(dish.id);
      setDishes((prev) => prev.filter((d) => d.id !== dish.id));
      showToast("Prato excluído.");
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "Não foi possível excluir o prato.";
      showToast(message, "error");
    }
  }

  return (
    <div className="app">
      <Header
        actions={
          <button type="button" className="btn" onClick={openNew}>
            + Novo prato
          </button>
        }
      />
      <CategoryChips active={activeCategory} onChange={setActiveCategory} />
      <DishGrid
        dishes={dishes}
        activeCategory={activeCategory}
        onEdit={openEdit}
        onDelete={handleDelete}
      />
      {modalOpen && (
        <DishFormModal
          initial={editingDish}
          onClose={() => setModalOpen(false)}
          onSave={handleSave}
        />
      )}
    </div>
  );
}
