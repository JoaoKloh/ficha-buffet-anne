import { CatalogView } from "@/components/CatalogView";
import { pratoBackend } from "@/lib/server/pratoBackend";
import type { PratoDetalhadoResponseDTO } from "@/lib/models/prato";

// Página sempre renderizada no servidor a cada requisição — o catálogo muda com frequência
// e não deve ficar em cache estático entre deploys.
export const dynamic = "force-dynamic";

export default async function CatalogPage() {
  // Fonte do catálogo: o backend real (PratoController). Se o backend estiver
  // fora do ar, cai para lista vazia em vez de derrubar a página inteira.
  let pratos: PratoDetalhadoResponseDTO[];
  try {
    pratos = await pratoBackend.listarTodos();
  } catch (err) {
    console.error("[CatalogPage] Falha ao carregar pratos do backend", err);
    pratos = [];
  }

  return <CatalogView initialPratos={pratos} />;
}
