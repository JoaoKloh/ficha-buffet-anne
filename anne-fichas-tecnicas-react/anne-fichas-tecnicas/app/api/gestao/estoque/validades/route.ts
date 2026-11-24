import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Insumos (pela validade da entrada) e pré-preparos vencidos ou vencendo em 7 dias. */
export async function GET() {
  return responder(() => estoqueBackend.validades());
}
