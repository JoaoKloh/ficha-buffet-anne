import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { responder } from "@/lib/server/gestao/http";
import { BackendError } from "@/lib/server/gestao/java";
import { HORIZONTES_COMPRA } from "@/lib/models/dominio";

export const dynamic = "force-dynamic";

/** Sugestão de compras para os eventos do horizonte (7, 15, 30 ou 60 dias) e os mínimos. */
export async function GET(req: Request) {
  return responder(() => {
    const h = Number(new URL(req.url).searchParams.get("horizonte") ?? 15);
    if (!HORIZONTES_COMPRA.some(([d]) => d === h)) throw new BackendError("Horizonte inválido.", 400);
    return estoqueBackend.compras(h);
  });
}
