"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Header } from "./Header";
import { ProductionFormModal } from "./ProductionFormModal";
import { useToast } from "./ui/Toast";
import { fmtDate } from "@/lib/utils/format";
import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";
import type {
  ProducaoResponseDTO,
  CreateProducaoRequestDTO,
  UpdateProducaoRequestDTO,
} from "@/lib/models/producao";

interface Props {
  initialProducoes: ProducaoResponseDTO[];
  pratos: PratoDetalhadoResponseDTO[];
}

export function ProductionListView({ initialProducoes, pratos }: Props) {
  const [producoes, setProducoes] = useState<ProducaoResponseDTO[]>(initialProducoes);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [editing, setEditing] = useState<ProducaoResponseDTO | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const { showToast } = useToast();
  const router = useRouter();

  function openNew() {
    if (pratos.length === 0) {
      showToast("Cadastre ao menos um prato no catálogo antes de criar uma produção.", "error");
      return;
    }
    setEditing(null);
    setModalOpen(true);
  }
  function openEdit(p: ProducaoResponseDTO, e: React.MouseEvent) {
    e.stopPropagation();
    setEditing(p);
    setModalOpen(true);
  }

  // POST -> Envia direto para /api/producao (proxy do ProducaoController real)
  async function handleCreate(input: CreateProducaoRequestDTO) {
    try {
      const response = await fetch("/api/producao", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "Não foi possível criar a produção.");
      }

      setModalOpen(false);
      showToast("Produção criada com sucesso no banco de dados!");

      // O backend responde 201 sem corpo (sem id) — recarrega a página do
      // Server Component para refletir a nova produção vinda do banco.
      window.location.reload();
    } catch (err) {
      const message = err instanceof Error ? err.message : "Não foi possível salvar a produção.";
      showToast(message, "error");
    }
  }

  // PUT -> Envia direto para /api/producao
  async function handleUpdate(input: UpdateProducaoRequestDTO) {
    try {
      const response = await fetch("/api/producao", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "Não foi possível atualizar a produção.");
      }

      setProducoes((prev) =>
        prev.map((p) =>
          p.id === input.id
            ? {
                ...p,
                nome: input.nome,
                quantidade: input.quantidade,
                data: input.data,
                pratos: pratos.filter((c) => input.pratos.some((sel) => sel.pratoId === c.id)),
              }
            : p
        )
      );
      setEditing(null);
      setModalOpen(false);
      showToast("Produção atualizada com sucesso!");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Não foi possível atualizar a produção.";
      showToast(message, "error");
    }
  }

  async function handleSave(input: CreateProducaoRequestDTO) {
    if (editing) {
      await handleUpdate({ ...input, id: editing.id });
    } else {
      await handleCreate(input);
    }
  }

  // DELETE -> Envia direto para /api/producao?id=X
  async function handleDelete(p: ProducaoResponseDTO, e: React.MouseEvent) {
    e.stopPropagation();
    if (!confirm(`Excluir a produção "${p.nome}"? Essa ação não pode ser desfeita.`)) return;
    try {
      const response = await fetch(`/api/producao?id=${p.id}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "Não foi possível excluir a produção.");
      }

      setProducoes((prev) => prev.filter((x) => x.id !== p.id));
      showToast("Produção excluída do banco de dados.");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Não foi possível excluir a produção.";
      showToast(message, "error");
    }
  }

  const query = searchQuery.trim().toLowerCase();
  const filtered = query
    ? producoes.filter((p) => p.nome.toLowerCase().includes(query))
    : producoes;
  const sorted = [...filtered].sort((a, b) => new Date(b.data).getTime() - new Date(a.data).getTime());

  return (
    <div className="app">
      <Header
        actions={
          <button type="button" className="btn" onClick={openNew}>
            + Nova produção
          </button>
        }
      />

      <div className="search-bar">
        <input
          type="search"
          placeholder="Buscar produção pelo nome do evento..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      {sorted.length === 0 ? (
        <div className="empty-state">
          {query
            ? "Nenhuma produção encontrada para essa busca."
            : (
              <>
                Nenhuma produção criada ainda. Clique em &quot;+ Nova produção&quot; para montar a lista de um
                evento.
              </>
            )}
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
                <div className="prod-name">{p.nome}</div>
                <div className="prod-meta">
                  {fmtDate(p.data)} · {p.quantidade ? `${p.quantidade} convidados · ` : ""}
                  {p.pratos.length} {p.pratos.length === 1 ? "prato" : "pratos"}
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
          onClose={() => setModalOpen(false)}
          onSave={handleSave}
        />
      )}
    </div>
  );
}
