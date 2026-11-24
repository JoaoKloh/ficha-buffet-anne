import { fichaBackend } from "@/lib/server/gestao/fichaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { CalcularCustoRequestSchema } from "@/lib/models/preparacao";

export const dynamic = "force-dynamic";

/** Custo do rascunho da ficha, para o resumo ao vivo do formulário. */
export async function POST(req: Request) {
  return responder(async () => fichaBackend.calcularCusto(await corpo(req, CalcularCustoRequestSchema)));
}
