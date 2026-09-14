import { notFound } from "next/navigation";
import { ProductionDetailView } from "@/components/ProductionDetailView";
import { dishRepository, productionRepository } from "@/lib/server/db";

export const dynamic = "force-dynamic";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function ProducaoDetailPage({ params }: Props) {
  const { id } = await params;
  const [production, dishes] = await Promise.all([
    productionRepository.getById(id),
    dishRepository.list(),
  ]);

  if (!production) {
    notFound();
  }

  return <ProductionDetailView production={production} dishes={dishes} />;
}
