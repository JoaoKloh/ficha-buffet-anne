"use client";

import { usePathname } from "next/navigation";
import { findActiveNav } from "@/lib/navigation";

// A navegação entre páginas fica na Sidebar; o Header só identifica a página
// atual (módulo + título, vindos de lib/navigation.ts) e hospeda as ações dela.
// `titulo` substitui o rótulo do menu em telas sem entrada própria nele (ex.: cronograma).
export function Header({ actions, titulo, sub }: { actions?: React.ReactNode; titulo?: string; sub?: React.ReactNode }) {
  const pathname = usePathname();
  const active = findActiveNav(pathname);

  return (
    <header className="top">
      <div className="brand">
        {active && <div className="tagline">{active.mod.label}</div>}
        <h1 className="page-title">{titulo ?? active?.item.label ?? "ANNE"}</h1>
        {sub && <p className="sub">{sub}</p>}
      </div>
      <div className="toolbar noprint">{actions}</div>
    </header>
  );
}
