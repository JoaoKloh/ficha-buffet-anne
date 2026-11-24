import type { Metadata } from "next";
import { Suspense } from "react";
import { CozinhaView } from "@/components/gestao/cozinha/CozinhaView";

export const metadata: Metadata = { title: "Cozinha — Anne" };

export default function CozinhaPage() {
  return (
    <Suspense>
      <CozinhaView />
    </Suspense>
  );
}
