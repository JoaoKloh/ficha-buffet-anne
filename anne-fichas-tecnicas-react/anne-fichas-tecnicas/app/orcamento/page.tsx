import type { Metadata } from "next";
import { OrcamentoView } from "@/components/OrcamentoView";

export const metadata: Metadata = {
  title: "Orçamento — Anne",
};

export default function OrcamentoPage() {
  return <OrcamentoView />;
}
