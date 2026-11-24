import type { Metadata } from "next";
import { PainelView } from "@/components/gestao/painel/PainelView";

export const metadata: Metadata = { title: "Painel — Anne" };

export default function PainelPage() {
  return <PainelView />;
}
