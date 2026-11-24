import { z } from "zod";
import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { responder } from "@/lib/server/gestao/http";
import { FORMS_OPCOES_ESTOQUE } from "@/lib/models/estoque";

export const dynamic = "force-dynamic";

const Para = z.enum(FORMS_OPCOES_ESTOQUE, { message: "Informe o formulário das opções (?para=)." });

/** Opções de um formulário do estoque (?para=): só os campos que ele usa. */
export async function GET(req: Request) {
  return responder(() => estoqueBackend.opcoes(Para.parse(new URL(req.url).searchParams.get("para"))));
}
