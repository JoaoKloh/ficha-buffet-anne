import { fichaBackend } from "@/lib/server/gestao/fichaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { CalcularCmvRequestSchema } from "@/lib/models/pratoVenda";

export const dynamic = "force-dynamic";

/** Custo e CMV do rascunho do prato, para o resumo ao vivo do formulário. */
export async function POST(req: Request) {
  return responder(async () => fichaBackend.calcularCmv(await corpo(req, CalcularCmvRequestSchema)));
}
