import type { Metadata } from "next";
import { ProducaoView } from "@/components/gestao/producao/ProducaoView";

export const metadata: Metadata = { title: "Produção — Anne" };

export default function ProducaoPage() {
  return <ProducaoView />;
}
