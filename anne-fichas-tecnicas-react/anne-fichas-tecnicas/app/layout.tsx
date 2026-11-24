import type { Metadata } from "next";
import { AppShell } from "@/components/AppShell";
import { ToastProvider } from "@/components/ui/Toast";
import { SheetProvider } from "@/components/ui/Sheet";
import "./globals.css";
import "./gestao.css";

export const metadata: Metadata = {
  title: "Anne Gestão",
  description: "Gestão do Buffet Anne: eventos, cozinha, fichas, estoque, caixa e DRE.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>
        <ToastProvider>
          <SheetProvider>
            <AppShell>{children}</AppShell>
          </SheetProvider>
        </ToastProvider>
      </body>
    </html>
  );
}
