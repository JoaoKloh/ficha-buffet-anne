import type { Metadata } from "next";
import { Suspense } from "react";
import { EventosView } from "@/components/gestao/eventos/EventosView";

export const metadata: Metadata = { title: "Eventos — Anne" };

export default function EventosPage() {
  return (
    <Suspense>
      <EventosView />
    </Suspense>
  );
}
