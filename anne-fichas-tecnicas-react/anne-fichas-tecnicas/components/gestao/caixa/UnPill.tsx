import { unCurto } from "@/lib/models/dominio";

/** Pílula da unidade de negócio; sem unidade aparece em amarelo para classificar. */
export function UnPill({ u }: { u: string | null | undefined }) {
  return <span className={`pill un-${u || "sem"}`}>{unCurto(u)}</span>;
}
