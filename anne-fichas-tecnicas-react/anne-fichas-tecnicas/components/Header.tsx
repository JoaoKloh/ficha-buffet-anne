"use client";

import { usePathname } from "next/navigation";
import { findActiveNav } from "@/lib/navigation";

// A navegação entre páginas fica na Sidebar; o Header só identifica a página
// atual (módulo + título, vindos de lib/navigation.ts) e hospeda as ações dela.
export function Header({ actions }: { actions?: React.ReactNode }) {
  const pathname = usePathname();
  const active = findActiveNav(pathname);

  return (
    <header className="top">
      <div className="brand">
        {active && <div className="tagline">{active.mod.label}</div>}
        <h1 className="page-title">{active?.item.label ?? "ANNE"}</h1>
      </div>
      <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>{actions}</div>
    </header>
  );
}
