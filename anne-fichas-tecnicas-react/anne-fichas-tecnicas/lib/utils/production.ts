import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";
import type { ProducaoResponseDTO } from "@/lib/models/producao";
import type {
  ProductionKitchenRow,
  ConsolidatedIngredientRow,
} from "@/lib/models/production";

/**
 * A produção real (ProducaoResponseDTO) não guarda uma quantidade por prato —
 * só a lista de pratos selecionados e o total de convidados do evento. A
 * quantidade a produzir de cada prato é sempre derivada aqui, na leitura:
 * convidados × sugestão por pessoa do prato (mesmo cálculo do botão
 * "Calcular quantidades pelos convidados" que existia no formulário antigo).
 */
function quantidadeAProduzir(producao: ProducaoResponseDTO, porPessoa: number): number {
  return Math.ceil(producao.quantidade * (porPessoa || 0));
}

export function buildKitchenRows(
  producao: ProducaoResponseDTO,
  pratos: PratoDetalhadoResponseDTO[]
): ProductionKitchenRow[] {
  const byId = new Map(pratos.map((p) => [p.id, p]));
  return producao.pratos.map((item) => {
    const prato = byId.get(item.id);
    return {
      dishId: String(item.id),
      nome: item.nome,
      categoria: item.categoria,
      foto: item.foto,
      quantidade: quantidadeAProduzir(producao, item.porPessoa),
      rendUnid: item.rendUnid,
      receita: prato?.receita ?? item.receita,
    };
  });
}

export function buildShoppingList(
  producao: ProducaoResponseDTO,
  pratos: PratoDetalhadoResponseDTO[]
): ConsolidatedIngredientRow[] {
  const byId = new Map(pratos.map((p) => [p.id, p]));
  const consolidated = new Map<string, ConsolidatedIngredientRow>();

  for (const item of producao.pratos) {
    const prato = byId.get(item.id);
    if (!prato) continue;
    const quantidade = quantidadeAProduzir(producao, item.porPessoa);
    const scale = quantidade / (prato.rendQtd || 1);
    for (const ing of prato.ingredientes) {
      const key = `${ing.nome.trim().toLowerCase()}|${ing.unidade.trim().toLowerCase()}`;
      const existing = consolidated.get(key);
      const add = (ing.qtd ?? 0) * scale;
      if (existing) {
        existing.total += add;
      } else {
        consolidated.set(key, { nome: ing.nome, unidade: ing.unidade, total: add });
      }
    }
  }

  return Array.from(consolidated.values()).sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"));
}
