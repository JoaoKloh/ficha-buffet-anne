"use client";

import { useState } from "react";
import { Header } from "./Header";
import { IngredientCategoryChips } from "./IngredientCategoryChips";
import { IngredientGrid } from "./IngredientGrid";
import { IngredientFormModal } from "./IngredientFormModal";
import { useToast } from "./ui/Toast";
import { ingredientsApi } from "@/lib/api/ingredients";
import { ApiError } from "@/lib/api/client";
import type { IngredienteResponseDTO, CreateIngredienteRequestDTO } from "@/lib/models/ingredient";

interface Props {
  initialIngredients?: IngredienteResponseDTO[];
}

export function IngredientListView({ initialIngredients = [] }: Props) {
  const [ingredients, setIngredients] = useState<IngredienteResponseDTO[]>(initialIngredients);
  const [activeCategory, setActiveCategory] = useState<string>("Todos");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [editingIngredient, setEditingIngredient] = useState<IngredienteResponseDTO | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const { showToast } = useToast();

  function openNew() {
    setEditingIngredient(null);
    setModalOpen(true);
  }

  function openEdit(ingredient: IngredienteResponseDTO) {
    setEditingIngredient(ingredient);
    setModalOpen(true);
  }

  // POST/PUT -> /api/ingredients (proxy do IngredientesController real)
  async function handleSave(input: CreateIngredienteRequestDTO) {
    try {
      if (editingIngredient) {
        const updated = await ingredientsApi.update(editingIngredient.id, {
          ...input,
          id: editingIngredient.id,
        });
        setIngredients((prev) => prev.map((i) => (i.id === updated.id ? updated : i)));
        showToast("Ingrediente atualizado com sucesso!");
      } else {
        await ingredientsApi.create(input);
        showToast("Ingrediente adicionado ao catálogo!");
        setModalOpen(false);
        // O backend responde 201 sem corpo (sem id) — recarrega a página do
        // Server Component para refletir o novo ingrediente vindo do banco.
        window.location.reload();
        return;
      }
      setModalOpen(false);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "Não foi possível salvar o ingrediente.";
      showToast(message, "error");
    }
  }

  // DELETE -> /api/ingredients/{id} (proxy do IngredientesController real)
  async function handleDelete(ingredient: IngredienteResponseDTO) {
    if (!confirm(`Excluir "${ingredient.nome}" do catálogo? Essa ação não pode ser desfeita.`))
      return;
    try {
      await ingredientsApi.remove(ingredient.id);
      setIngredients((prev) => prev.filter((i) => i.id !== ingredient.id));
      showToast("Ingrediente excluído do banco de dados.");
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "Não foi possível excluir o ingrediente.";
      showToast(message, "error");
    }
  }

  return (
    <div className="app">
      <Header
        actions={
          <button type="button" className="btn" onClick={openNew}>
            + Novo ingrediente
          </button>
        }
      />
      <div className="search-bar">
        <input
          type="search"
          placeholder="Buscar ingrediente pelo nome..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>
      <IngredientCategoryChips active={activeCategory} onChange={setActiveCategory} />
      <IngredientGrid
        ingredients={ingredients}
        activeCategory={activeCategory}
        searchQuery={searchQuery}
        onEdit={openEdit}
        onDelete={handleDelete}
      />
      {modalOpen && (
        <IngredientFormModal
          initial={editingIngredient}
          onClose={() => setModalOpen(false)}
          onSave={handleSave}
        />
      )}
    </div>
  );
}