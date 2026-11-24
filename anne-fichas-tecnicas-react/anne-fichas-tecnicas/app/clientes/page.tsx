import type { Metadata } from "next";
import { ClientesView } from "@/components/gestao/clientes/ClientesView";

export const metadata: Metadata = { title: "Clientes — Anne" };

export default function ClientesPage() {
  return <ClientesView />;
}
