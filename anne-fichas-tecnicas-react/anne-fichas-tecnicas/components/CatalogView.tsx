"use client";

import { useState } from "react";
import { Header } from "./Header";
import { CategoryChips } from "./CategoryChips";
import { DishGrid } from "./DishGrid";
import { DishFormModal } from "./DishFormModal";
import { PratoEditModal } from "./PratoEditModal";
import { AssociateProductionModal } from "./AssociateProductionModal";
import { useToast } from "./ui/Toast";
import type { CreateDishInput } from "@/lib/models/dish";
import type {
  PratoDetalhadoResponseDTO,
  UpdatePratoRequest,
  AssociationPratoProducaoRequestDTO,
} from "@/lib/models/prato";

interface Props {
  initialPratos: PratoDetalhadoResponseDTO[];
}

export function CatalogView({ initialPratos }: Props) {
  const [pratos, setPratos] = useState<PratoDetalhadoResponseDTO[]>(initialPratos);
  const [activeCategory, setActiveCategory] = useState<string>("Todos");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editingPrato, setEditingPrato] = useState<PratoDetalhadoResponseDTO | null>(null);
  const [associatingPrato, setAssociatingPrato] = useState<PratoDetalhadoResponseDTO | null>(null);
  const { showToast } = useToast();

  // 1. POST -> Envia para /api/prato (sem 404, sem CORS)
  async function handleCreate(input: CreateDishInput) {
    try {
      const response = await fetch("/api/prato", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });
  
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "Não foi possível criar o prato.");
      }
  
      setCreateModalOpen(false);
      showToast("Prato criado com sucesso no banco de dados!");
      
      // Recarrega o Server Component (page.tsx) para refletir o novo prato vindo do banco
      window.location.reload(); 
    } catch (err) {
      const message = err instanceof Error ? err.message : "Não foi possível salvar o prato.";
      showToast(message, "error");
    }
  }

  // 2. PUT -> Envia para /api/prato
  async function handleUpdate(input: UpdatePratoRequest) {
    try {
      const response = await fetch("/api/prato", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "Não foi possível atualizar o prato.");
      }

      setPratos((prev) =>
        prev.map((p) =>
          p.id === input.id
            ? {
                ...p,
                nome: input.nome,
                categoria: input.categoria,
                foto: input.foto ?? "",
                rendQtd: input.rendQtd,
                rendUnid: input.rendUnid,
                porPessoa: input.porPessoa,
                receita: input.receita,
                ingredientes: p.ingredientes.filter((i) =>
                  input.ingredientes.some((sel) => sel.ingredienteId === i.id)
                ),
              }
            : p
        )
      );
      setEditingPrato(null);
      showToast("Prato atualizado com sucesso!");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Não foi possível atualizar o prato.";
      showToast(message, "error");
    }
  }

  // 3. DELETE -> Envia para /api/prato?id=X
  async function handleDelete(prato: PratoDetalhadoResponseDTO) {
    if (!confirm(`Excluir "${prato.nome}" do catálogo? Essa ação não pode ser desfeita.`)) return;
    try {
      const response = await fetch(`/api/prato?id=${prato.id}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "Não foi possível excluir o prato.");
      }

      setPratos((prev) => prev.filter((p) => p.id !== prato.id));
      showToast("Prato excluído do banco de dados.");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Não foi possível excluir o prato.";
      showToast(message, "error");
    }
  }

  // POST -> Envia para /api/prato/associar-eventos (proxy do PratoController real)
  async function handleSaveAssociation(input: AssociationPratoProducaoRequestDTO) {
    try {
      const response = await fetch("/api/prato/associar-eventos", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "Não foi possível associar o prato aos eventos.");
      }

      setAssociatingPrato(null);
      showToast("Prato associado aos eventos selecionados!");
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Não foi possível associar o prato aos eventos.";
      showToast(message, "error");
    }
  }

  return (
    <div className="app">
      <Header
        actions={
          <button type="button" className="btn" onClick={() => setCreateModalOpen(true)}>
            + Novo prato
          </button>
        }
      />
      <div className="search-bar">
        <input
          type="search"
          placeholder="Buscar prato pelo nome..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>
      <CategoryChips active={activeCategory} onChange={setActiveCategory} />
      <DishGrid
        dishes={pratos}
        activeCategory={activeCategory}
        searchQuery={searchQuery}
        onEdit={setEditingPrato}
        onDelete={handleDelete}
        onAssociateProduction={setAssociatingPrato}
      />
      {createModalOpen && (
        <DishFormModal initial={null} onClose={() => setCreateModalOpen(false)} onSave={handleCreate} />
      )}
      {editingPrato && (
        <PratoEditModal prato={editingPrato} onClose={() => setEditingPrato(null)} onSave={handleUpdate} />
      )}
      {associatingPrato && (
        <AssociateProductionModal
          dish={{ id: associatingPrato.id, nome: associatingPrato.nome }}
          onClose={() => setAssociatingPrato(null)}
          onSave={handleSaveAssociation}
        />
      )}
    </div>
  );
}