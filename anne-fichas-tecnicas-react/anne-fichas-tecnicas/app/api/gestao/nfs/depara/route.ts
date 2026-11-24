import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { NfDeParaRequestSchema } from "@/lib/models/estoque";

export const dynamic = "force-dynamic";

/** Sugere o item do cadastro de cada linha pelo histórico de notas do fornecedor. */
export async function POST(req: Request) {
  return responder(async () => estoqueBackend.dePara(await corpo(req, NfDeParaRequestSchema)));
}
