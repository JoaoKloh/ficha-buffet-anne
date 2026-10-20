import { notFound } from "next/navigation";
import { ProductionDetailView } from "@/components/ProductionDetailView";
import { producaoBackend } from "@/lib/server/producaoBackend";
import { pratoBackend } from "@/lib/server/pratoBackend";

export const dynamic = "force-dynamic";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function ProducaoDetailPage({ params }: Props) {
  const { id } = await params;

  // ProducaoController não tem endpoint de busca por id — busca a lista
  // completa (mesma fonte da tela de listagem) e filtra pelo id da rota.
  const [producoes, pratos] = await Promise.all([
    producaoBackend.listarTodos().catch(() => []),
    pratoBackend.listarTodos().catch(() => []),
  ]);

  const producao = producoes.find((p) => String(p.id) === id);
  if (!producao) {
    notFound();
  }

  return <ProductionDetailView producao={producao} pratos={pratos} />;
}
