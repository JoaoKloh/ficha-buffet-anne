"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export function Header({ actions }: { actions?: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <>
      <header className="top">
        <div className="brand">
          <div className="mark">ANNE</div>
          <div className="tagline">Fichas técnicas &amp; produção</div>
        </div>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>{actions}</div>
      </header>

      <nav className="tabs">
        <Link href="/" className={`tab ${pathname === "/" ? "active" : ""}`}>
          Catálogo de pratos
        </Link>
        <Link
          href="/producao"
          className={`tab ${pathname?.startsWith("/producao") ? "active" : ""}`}
        >
          Produção por evento
        </Link>
      </nav>
    </>
  );
}
