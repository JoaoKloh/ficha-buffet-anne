"use client";

import { useEffect, useState } from "react";
import { Header } from "./Header";
import { FinanceiroPeriodo } from "./FinanceiroPeriodo";
import { FinanceiroResumo } from "./FinanceiroResumo";
import { FinanceiroDre } from "./FinanceiroDre";
import { FinanceiroRelatorio } from "./FinanceiroRelatorio";
import { LancamentoForm } from "./LancamentoForm";
import { LancamentoList, type FiltrosLancamento } from "./LancamentoList";
import { LancamentoFormModal } from "./LancamentoFormModal";
import { mensagemDeErro } from "./LancamentoCampos";
import { useToast } from "./ui/Toast";
import {
  atualizarLancamento,
  criarLancamento,
  excluirLancamento,
  listarLancamentos,
} from "@/lib/api/lancamentos";
import { listarProducoesDisponiveis } from "@/lib/api/pratoProducoes";
import type { Lancamento, NovoLancamento } from "@/lib/models/financeiro";
import type { ProducaoResponseDTO } from "@/lib/models/producao";
import { noPeriodo, periodoAtual, rotuloPeriodo, type Periodo } from "@/lib/utils/financeiro";

type Aba = "lancar" | "lista" | "dre";

const ABAS: { id: Aba; rotulo: string }[] = [
  { id: "lancar", rotulo: "Lançar" },
  { id: "lista", rotulo: "Lançamentos" },
  { id: "dre", rotulo: "DRE" },
];

export function FinanceiroView() {
  const [lancamentos, setLancamentos] = useState<Lancamento[]>([]);
  const [producoes, setProducoes] = useState<ProducaoResponseDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [periodo, setPeriodo] = useState<Periodo>(periodoAtual);
  const [aba, setAba] = useState<Aba>("lancar");
  const [filtros, setFiltros] = useState<FiltrosLancamento>({ categoria: "", producaoId: "" });
  const [editando, setEditando] = useState<Lancamento | null>(null);
  const { showToast } = useToast();

  useEffect(() => {
    listarLancamentos()
      .then(setLancamentos)
      .catch((err) => showToast(mensagemDeErro(err, "Não foi possível carregar os lançamentos."), "error"))
      .finally(() => setLoading(false));

    // Só alimenta o seletor "Produção vinculada": uma falha aqui não impede usar o Financeiro.
    listarProducoesDisponiveis()
      .then(setProducoes)
      .catch((err) => showToast(mensagemDeErro(err, "Não foi possível carregar as produções."), "error"));
  }, [showToast]);

  // POST -> /api/lancamentos. Sem try/catch aqui: o erro sobe até o formulário,
  // que mostra a mensagem do backend ao lado dos campos.
  async function handleCreate(input: NovoLancamento) {
    const novo = await criarLancamento(input);
    setLancamentos((prev) => [novo, ...prev]);
    // Leva a régua para o mês do lançamento, para ele aparecer nos cartões e na lista.
    const [ano, mes] = novo.data.split("-").map(Number);
    if (ano && mes && noPeriodo([novo], periodo).length === 0) {
      setPeriodo({ ano, mes: mes - 1, anoInteiro: false });
    }
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

  function abrirCategoria(categoria: string) {
    setFiltros({ categoria, producaoId: "" });
    setAba("lista");
  }

  const doPeriodo = noPeriodo(lancamentos, periodo);
  // Os maiores ids são os lançados por último.
  const recentes = [...lancamentos].sort((a, b) => b.id - a.id).slice(0, 3);
  const titulo = rotuloPeriodo(periodo);

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
          <div className="fin-stack fin-coluna">
            <p className="fin-muted fin-intro">
              Lance cada receita e cada despesa. A DRE do período se monta sozinha.
            </p>

            <FinanceiroPeriodo value={periodo} onChange={setPeriodo} />
            <FinanceiroResumo lancamentos={doPeriodo} />

            <div className="segmented fin-abas" role="tablist" aria-label="Seções do financeiro">
              {ABAS.map((a) => (
                <button
                  key={a.id}
                  type="button"
                  role="tab"
                  id={`fin-aba-${a.id}`}
                  aria-selected={aba === a.id}
                  aria-controls={`fin-painel-${a.id}`}
                  className={aba === a.id ? "active" : ""}
                  onClick={() => setAba(a.id)}
                >
                  {a.rotulo}
                </button>
              ))}
            </div>

            <div role="tabpanel" id={`fin-painel-${aba}`} aria-labelledby={`fin-aba-${aba}`}>
              {aba === "lancar" && (
                <LancamentoForm
                  onSave={handleCreate}
                  producoes={producoes}
                  recentes={recentes}
                  onEdit={setEditando}
                  onDelete={handleDelete}
                />
              )}
              {aba === "lista" && (
                <LancamentoList
                  lancamentos={doPeriodo}
                  producoes={producoes}
                  filtros={filtros}
                  onFiltros={setFiltros}
                  onEdit={setEditando}
                  onDelete={handleDelete}
                />
              )}
              {aba === "dre" && (
                <FinanceiroDre
                  lancamentos={doPeriodo}
                  todos={lancamentos}
                  periodo={periodo}
                  onAbrirCategoria={abrirCategoria}
                />
              )}
            </div>
          </div>
        )}

        {editando && (
          <LancamentoFormModal
            key={editando.id}
            initial={editando}
            onClose={() => setEditando(null)}
            onSave={handleUpdate}
            producoes={producoes}
          />
        )}
      </div>

      {!loading && (
        <FinanceiroRelatorio
          lancamentos={doPeriodo}
          periodo={titulo.charAt(0).toUpperCase() + titulo.slice(1)}
        />
      )}
    </div>
  );
}
