import type { Metadata } from "next";
import { FinanceiroView } from "@/components/FinanceiroView";

export const metadata: Metadata = {
  title: "Financeiro — Anne",
};

export default function FinanceiroPage() {
  return <FinanceiroView />;
}
