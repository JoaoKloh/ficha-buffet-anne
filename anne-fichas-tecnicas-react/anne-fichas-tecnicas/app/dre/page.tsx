import type { Metadata } from "next";
import { DreView } from "@/components/gestao/dre/DreView";

export const metadata: Metadata = { title: "DRE — Anne" };

export default function DrePage() {
  return <DreView />;
}
