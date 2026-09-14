"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Header } from "./Header";
import { ProductionFormModal } from "./ProductionFormModal";
import { useToast } from "./ui/Toast";
import { productionsApi } from "@/lib/api/productions";
import { ApiError } from "@/lib/api/client";
import { fmtDate } from "@/lib/utils/format";
import type { Dish } from "@/lib/models/dish";
import type { Production, CreateProductionInput } from "@/lib/models/production";

interface Props {
  initialProductions: Production[];
  dishes: Dish[];
}

export function ProductionListView({ initialProductions, dishes }: Props) {
  const [productions, setProductions] = useState<Production[]>(initialProductions);
  const [editing, setEditing] = useState<Production | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const { showToast } = useToast();
  const router = useRouter();

  function openNew() {
    if (dishes.length === 0) {
      showToast("Cadastre ao menos um prato no catálogo antes de criar uma produção.", "error");
      return;
    }
    setEditing(null);
    setModalOpen(true);
  }
  function openEdit(p: Production, e: React.MouseEvent) {
    e.stopPropagation();
    setEditing(p);
    setModalOpen(true);
  }

  async function handleSave(input: CreateProductionInput) {
    try {
      if (editing) {
        const updated = await productionsApi.update(editing.id, input);
        setProductions((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
        showToast("Produção atualizada.");
        setModalOpen(false);
      } else {
        const created = await productionsApi.create(input);
        setProductions((prev) => [...prev, created]);
        showToast("Produção criada.");
        setModalOpen(false);
        router.push(`/producao/${created.id}`);
      }
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "Não foi possível salvar a produção.";
      showToast(message, "error");
    }
  }

  async function handleDelete(p: Production, e: React.MouseEvent) {
    e.stopPropagation();
    if (!confirm(`Excluir a produção "${p.evento}"? Essa ação não pode ser desfeita.`)) return;
    try {
      await productionsApi.remove(p.id);
      setProductions((prev) => prev.filter((x) => x.id !== p.id));
      showToast("Produção excluída.");
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "Não foi possível excluir a produção.";
      showToast(message, "error");
    }
  }

  const sorted = [...productions].sort(
    (a, b) => new Date(b.data || b.createdAt).getTime() - new Date(a.data || a.createdAt).getTime()
  );

  return (
    <div className="app">
      <Header
        actions={
          <button type="button" className="btn" onClick={openNew}>
            + Nova produção
          </button>
        }
      />

      {sorted.length === 0 ? (
        <div className="empty-state">
          Nenhuma produção criada ainda. Clique em &quot;+ Nova produção&quot; para montar a lista de um
          evento.
        </div>
      ) : (
        <div className="prod-list">
          {sorted.map((p) => (
            <div
              key={p.id}
              className="prod-card"
              onClick={() => router.push(`/producao/${p.id}`)}
            >
              <div>
                <div className="prod-name">{p.evento}</div>
                <div className="prod-meta">
                  {fmtDate(p.data)} · {p.convidados ? `${p.convidados} convidados · ` : ""}
                  {p.itens.length} {p.itens.length === 1 ? "item" : "itens"}
                </div>
              </div>
              <div className="prod-card-actions">
                <button type="button" className="btn small ghost" onClick={(e) => openEdit(p, e)}>
                  Editar
                </button>
                <button type="button" className="btn small danger" onClick={(e) => handleDelete(p, e)}>
                  Excluir
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalOpen && (
        <ProductionFormModal
          initial={editing}
          dishes={dishes}
          onClose={() => setModalOpen(false)}
          onSave={handleSave}
        />
      )}
    </div>
  );
}
