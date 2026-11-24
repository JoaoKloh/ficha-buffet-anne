import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { responder } from "@/lib/server/gestao/http";
import { BackendError } from "@/lib/server/gestao/java";

export const dynamic = "force-dynamic";

/** Perdas, avarias e descartes de lote do mês (AAAA-MM), com valor. */
export async function GET(req: Request) {
  return responder(() => {
    const mes = new URL(req.url).searchParams.get("mes") ?? "";
    if (!/^\d{4}-\d{2}$/.test(mes)) throw new BackendError("Mês inválido.", 400);
    return estoqueBackend.perdas(mes);
  });
}
