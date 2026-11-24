import type { Metadata } from "next";
import { Header } from "@/components/Header";
import { BackupPainel } from "@/components/gestao/painel/Backup";

export const metadata: Metadata = { title: "Backup — Anne" };

export default function BackupPage() {
  return (
    <div className="app">
      <Header titulo="Backup e restauração" sub="Cópia de tudo que está cadastrado, no seu aparelho" />
      <section className="panel">
        <div className="sheet-b" style={{ overflow: "visible" }}>
          <BackupPainel />
        </div>
      </section>
    </div>
  );
}
