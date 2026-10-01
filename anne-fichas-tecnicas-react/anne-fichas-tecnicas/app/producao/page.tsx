import { ProductionListView } from "@/components/ProductionListView";
import { producaoBackend } from "@/lib/server/producaoBackend";
import { pratoBackend } from "@/lib/server/pratoBackend";
import type { ProducaoResponseDTO } from "@/lib/models/producao";
import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";

// Página sempre renderizada no servidor a cada requisição — a lista de produções
// muda com frequência e não deve ficar em cache estático entre deploys.
export const dynamic = "force-dynamic";

export default async function ProducaoPage() {
  // Fonte das produções e do catálogo de pratos: o backend real
  // (ProducaoController/PratoController). Se o backend estiver fora do ar,
  // cai para lista vazia em vez de derrubar a página inteira.
  let producoes: ProducaoResponseDTO[];
  try {
    producoes = await producaoBackend.listarTodos();
  } catch (err) {
    console.error("[ProducaoPage] Falha ao carregar produções do backend", err);
    producoes = [];
  }

  let pratos: PratoDetalhadoResponseDTO[];
  try {
    pratos = await pratoBackend.listarTodos();
  } catch (err) {
    console.error("[ProducaoPage] Falha ao carregar pratos do backend", err);
    pratos = [];
  }

  return <ProductionListView initialProducoes={producoes} pratos={pratos} />;
}
