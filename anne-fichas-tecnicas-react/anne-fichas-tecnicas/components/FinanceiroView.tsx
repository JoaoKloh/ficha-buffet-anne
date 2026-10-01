"use client";

import { useEffect, useState } from "react";
import { Header } from "./Header";
import { FinanceiroResumo } from "./FinanceiroResumo";
import { FinanceiroRelatorio } from "./FinanceiroRelatorio";
import { LancamentoForm } from "./LancamentoForm";
import { LancamentoList } from "./LancamentoList";
import { LancamentoFormModal } from "./LancamentoFormModal";
import { mensagemDeErro } from "./LancamentoCampos";
import { useToast } from "./ui/Toast";
import {
  atualizarLancamento,
  criarLancamento,
  excluirLancamento,
  listarLancamentos,
} from "@/lib/api/lancamentos";
import type { Lancamento, NovoLancamento } from "@/lib/models/financeiro";
import { fmtMes, mesDe, mesesDisponiveis } from "@/lib/utils/financeiro";

const TODOS = "todos";

export function FinanceiroView() {
  const [lancamentos, setLancamentos] = useState<Lancamento[]>([]);
  const [loading, setLoading] = useState(true);
  const [mesFiltro, setMesFiltro] = useState<string>(TODOS);
  const [editando, setEditando] = useState<Lancamento | null>(null);
  const { showToast } = useToast();

  useEffect(() => {
    listarLancamentos()
      .then(setLancamentos)
      .catch((err) => showToast(mensagemDeErro(err, "Não foi possível carregar os lançamentos."), "error"))
      .finally(() => setLoading(false));
  }, [showToast]);

  // POST -> /api/lancamentos. Sem try/catch aqui: o erro sobe até o formulário,
  // que mostra a mensagem do backend ao lado dos campos.
  async function handleCreate(input: NovoLancamento) {
    const novo = await criarLancamento(input);
    setLancamentos((prev) => [novo, ...prev]);
    showToast("Lançamento adicionado.");
  }

  // PUT -> /api/lancamentos (o id vai no corpo, como em AtualizarLancamentoDTO).
  async function handleUpdate(input: NovoLancamento) {
    if (!editando) return;
    const atualizado = await atualizarLancamento({ ...input, id: editando.id });
    setLancamentos((prev) => prev.map((l) => (l.id === atualizado.id ? atualizado : l)));
    setEditando(null);
    showToast("Lançamento atualizado.");
  }

  async function handleDelete(l: Lancamento) {
    if (!confirm(`Excluir o lançamento "${l.descricao}"? Essa ação não pode ser desfeita.`)) return;
    try {
      await excluirLancamento(l.id);
      setLancamentos((prev) => prev.filter((x) => x.id !== l.id));
      showToast("Lançamento excluído.");
    } catch (err) {
      showToast(mensagemDeErro(err, "Não foi possível excluir o lançamento."), "error");
    }
  }

  const meses = mesesDisponiveis(lancamentos);
  // Se o mês filtrado deixou de ter lançamentos (ex.: excluiu o último), volta para "Todos".
  const filtroAtivo = mesFiltro !== TODOS && meses.includes(mesFiltro) ? mesFiltro : TODOS;
  const filtrados =
    filtroAtivo === TODOS ? lancamentos : lancamentos.filter((l) => mesDe(l.data) === filtroAtivo);

  return (
    <div className="app">
      <div className="no-print">
        <Header
          actions={
            !loading && (
              <button type="button" className="btn ghost small" onClick={() => window.print()}>
                Imprimir relatório
              </button>
            )
          }
        />

        {loading ? (
          <p className="empty-state">Carregando lançamentos...</p>
        ) : (
          <div className="fin-stack">
            <LancamentoForm onSave={handleCreate} />

            {meses.length > 0 && (
              <div className="chips fin-periodo" role="group" aria-label="Filtrar por período">
                <span className="fin-muted">Período:</span>
                {[TODOS, ...meses].map((m) => (
                  <button
                    key={m}
                    type="button"
                    className={`chip ${filtroAtivo === m ? "active" : ""}`}
                    aria-pressed={filtroAtivo === m}
                    onClick={() => setMesFiltro(m)}
                  >
                    {m === TODOS ? "Todos" : fmtMes(m)}
                  </button>
                ))}
              </div>
            )}

            <FinanceiroResumo lancamentos={filtrados} />
            <LancamentoList lancamentos={filtrados} onEdit={setEditando} onDelete={handleDelete} />
          </div>
        )}

        {editando && (
          <LancamentoFormModal
            key={editando.id}
            initial={editando}
            onClose={() => setEditando(null)}
            onSave={handleUpdate}
          />
        )}
      </div>

      {!loading && (
        <FinanceiroRelatorio
          lancamentos={filtrados}
          periodo={filtroAtivo === TODOS ? "Todos os lançamentos" : fmtMes(filtroAtivo)}
        />
      )}
    </div>
  );
}
