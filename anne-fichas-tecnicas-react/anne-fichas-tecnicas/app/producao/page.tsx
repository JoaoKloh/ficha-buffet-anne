import { ProductionListView } from "@/components/ProductionListView";
import { dishRepository, productionRepository } from "@/lib/server/db";

export const dynamic = "force-dynamic";

export default async function ProducaoPage() {
  const [productions, dishes] = await Promise.all([
    productionRepository.list(),
    dishRepository.list(),
  ]);

  return <ProductionListView initialProductions={productions} dishes={dishes} />;
}
