import type { Metadata } from "next";
import { Suspense } from "react";
import { CaixaView } from "@/components/gestao/caixa/CaixaView";

export const metadata: Metadata = { title: "Caixa — Anne" };

export default function CaixaPage() {
  return (
    <Suspense>
      <CaixaView />
    </Suspense>
  );
}
