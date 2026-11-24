import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Lançamentos ainda sem unidade de negócio. */
export async function GET() {
  return responder(() => caixaBackend.semUnidade(), 200, "Erro ao carregar os lançamentos sem unidade.");
}
