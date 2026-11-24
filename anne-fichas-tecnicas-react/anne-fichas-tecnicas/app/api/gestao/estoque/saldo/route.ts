import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Saldo físico, reservado, disponível e situação de cada item. */
export async function GET() {
  return responder(() => estoqueBackend.saldo(), 200, "Erro ao calcular o estoque.");
}
