import { apiClient } from "./client";
import type {
  CreateOrcamentoRequest,
  Orcamento,
  OrcamentoDetalhadoDTO,
  OrcamentoSalvo,
} from "@/lib/models/orcamento";

/**
 * Persistência dos orçamentos.
 *
 * Os orçamentos salvos vão para /api/orcamento (mesma origem do frontend, proxy do
 * OrcamentoController real) — mesmo padrão de /api/lancamentos. O backend guarda o
 * cliente e os totais; a composição completa (cardápio, equipe, margens...) que o
 * "Abrir" e o resumo precisam fica no localStorage, indexada pelo id do backend,
 * assim como o rascunho em edição.
 */

const RASCUNHO_KEY = "anne:orcamento-rascunho";
const DETALHES_KEY = "anne:orcamentos-detalhes";

type Detalhes = Record<number, Orcamento>;

function ler<T>(key: string): T | null {
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}

/** Falhar aqui não deve interromper o usuário: o orçamento já está salvo no backend. */
function gravarDetalhes(detalhes: Detalhes): void {
  try {
    window.localStorage.setItem(DETALHES_KEY, JSON.stringify(detalhes));
  } catch {
    // armazenamento cheio/bloqueado: só os totais do backend ficam disponíveis
  }
}

export async function carregarRascunho(): Promise<Orcamento | null> {
  return ler<Orcamento>(RASCUNHO_KEY);
}

/** Chamado a cada edição; falhar aqui não deve interromper o usuário. */
export function salvarRascunho(orcamento: Orcamento): void {
  try {
    window.localStorage.setItem(RASCUNHO_KEY, JSON.stringify(orcamento));
  } catch {
    // armazenamento cheio/bloqueado: o orçamento segue em memória
  }
}

/** Mais recentes primeiro (o id do backend é sequencial). */
export async function listarOrcamentos(): Promise<OrcamentoSalvo[]> {
  const lista = await apiClient.get<OrcamentoDetalhadoDTO[]>("/api/orcamento");
  const detalhes = ler<Detalhes>(DETALHES_KEY) ?? {};
  return lista
    .map((dto) => ({ ...dto, orcamento: detalhes[dto.id] ?? null }))
    .sort((a, b) => b.id - a.id);
}

export async function salvarOrcamento(
  input: CreateOrcamentoRequest,
  orcamento: Orcamento
): Promise<OrcamentoSalvo> {
  const criado = await apiClient.post<OrcamentoDetalhadoDTO>("/api/orcamento", input);
  gravarDetalhes({ ...(ler<Detalhes>(DETALHES_KEY) ?? {}), [criado.id]: orcamento });
  return { ...criado, orcamento };
}

export async function excluirOrcamento(id: number): Promise<void> {
  await apiClient.delete<void>(`/api/orcamento?id=${id}`);
  const detalhes = ler<Detalhes>(DETALHES_KEY);
  if (detalhes && id in detalhes) {
    delete detalhes[id];
    gravarDetalhes(detalhes);
  }
}
