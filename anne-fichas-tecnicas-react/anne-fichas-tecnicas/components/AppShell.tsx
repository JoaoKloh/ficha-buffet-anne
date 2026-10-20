"use client";

import { usePathname } from "next/navigation";
import { Sidebar } from "./Sidebar";

// Mesmas rotas públicas do middleware.ts: nelas não há sessão, então nada de sidebar.
const BARE_PATHS = ["/login"];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  if (pathname && BARE_PATHS.includes(pathname)) {
    return <>{children}</>;
  }

  return (
    <div className="shell">
      <Sidebar />
      <main className="shell-main">{children}</main>
    </div>
  );
}
