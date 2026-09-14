import { CatalogView } from "@/components/CatalogView";
import { dishRepository } from "@/lib/server/db";
import { buildSeedDishes } from "@/lib/server/seedData";

// Página sempre renderizada no servidor a cada requisição — o catálogo muda com frequência
// e não deve ficar em cache estático entre deploys.
export const dynamic = "force-dynamic";

export default async function CatalogPage() {
  // Server Components podem acessar a camada de dados diretamente — evita um
  // round-trip HTTP desnecessário no primeiro carregamento. As mutações do
  // usuário (criar/editar/excluir), essas sim, sempre passam pela API HTTP
  // (ver components/CatalogView.tsx → lib/api/dishes.ts), como pedido.
  await dishRepository.seedIfEmpty(buildSeedDishes());
  const dishes = await dishRepository.list();

  return <CatalogView initialDishes={dishes} />;
}
