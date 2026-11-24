import type { Metadata } from "next";
import { FichasView } from "@/components/gestao/fichas/FichasView";

export const metadata: Metadata = { title: "Fichas técnicas — Anne" };

export default function FichasPage() {
  return <FichasView />;
}
