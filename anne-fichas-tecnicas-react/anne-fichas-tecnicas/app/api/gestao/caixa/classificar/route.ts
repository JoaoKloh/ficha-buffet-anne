import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { ClassificarRequestSchema } from "@/lib/models/caixa";

export const dynamic = "force-dynamic";

/** Põe lançamentos numa unidade de negócio. */
export async function PUT(req: Request) {
  return responder(async () => caixaBackend.classificar(await corpo(req, ClassificarRequestSchema)));
}
