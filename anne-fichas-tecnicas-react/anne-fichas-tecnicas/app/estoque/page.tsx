import type { Metadata } from "next";
import { Suspense } from "react";
import { EstoqueView } from "@/components/gestao/estoque/EstoqueView";

export const metadata: Metadata = { title: "Estoque — Anne" };

export default function EstoquePage() {
  return (
    <Suspense>
      <EstoqueView />
    </Suspense>
  );
}
