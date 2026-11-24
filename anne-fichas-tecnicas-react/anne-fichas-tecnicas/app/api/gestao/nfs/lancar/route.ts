import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { corpo, idDe, responder } from "@/lib/server/gestao/http";
import { SalvarNotaFiscalRequestSchema } from "@/lib/models/estoque";

export const dynamic = "force-dynamic";

/** Dar entrada (rascunho ?id= ou nota nova): estoque, preço dos itens e contas a pagar. */
export async function POST(req: Request) {
  const id = new URL(req.url).searchParams.get("id");
  return responder(async () => estoqueBackend.lancarNf(id ? idDe(id) : null, await corpo(req, SalvarNotaFiscalRequestSchema)));
}
