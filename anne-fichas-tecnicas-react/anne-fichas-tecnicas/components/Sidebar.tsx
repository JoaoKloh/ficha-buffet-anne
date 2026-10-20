"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { NAV_MODULES, isNavItemActive } from "@/lib/navigation";

export function Sidebar() {
  const pathname = usePathname();
  // Só relevante em telas pequenas, onde a sidebar vira uma gaveta.
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [open]);

  return (
    <>
      <div className="mobile-bar no-print">
        <button
          type="button"
          className="sidebar-toggle"
          aria-label="Abrir menu"
          aria-expanded={open}
          aria-controls="app-sidebar"
          onClick={() => setOpen(true)}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path d="M3 6h18M3 12h18M3 18h18" strokeLinecap="round" />
          </svg>
        </button>
        <div className="mark">ANNE</div>
      </div>

      {open && <div className="sidebar-backdrop no-print" onClick={() => setOpen(false)} />}

      <aside id="app-sidebar" className={`sidebar no-print ${open ? "open" : ""}`}>
        <div className="brand sidebar-brand">
          <div className="mark">ANNE</div>
          <div className="tagline">Buffet</div>
        </div>

        <nav aria-label="Navegação principal">
          {NAV_MODULES.map((mod) => (
            <div key={mod.label} className="sidebar-group">
              <div className="sidebar-group-label">{mod.label}</div>
              {mod.items.map((item) => {
                const active = isNavItemActive(item.href, pathname);
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`sidebar-link ${active ? "active" : ""}`}
                    aria-current={active ? "page" : undefined}
                    onClick={() => setOpen(false)}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </div>
          ))}
        </nav>
      </aside>
    </>
  );
}
