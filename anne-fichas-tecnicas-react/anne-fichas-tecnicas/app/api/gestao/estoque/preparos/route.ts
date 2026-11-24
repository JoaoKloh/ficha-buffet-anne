import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Pré-preparos: KPIs, pronto agora (FIFO) e lotes filtrados. */
export async function GET(req: Request) {
  return responder(() => estoqueBackend.preparos(new URL(req.url).searchParams.get("filtro") ?? "ativos"));
}
