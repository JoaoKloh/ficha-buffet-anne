import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Integridade do estoque: movimentos por origem e pontos para conferir. */
export async function GET() {
  return responder(() => estoqueBackend.conferencia());
}
