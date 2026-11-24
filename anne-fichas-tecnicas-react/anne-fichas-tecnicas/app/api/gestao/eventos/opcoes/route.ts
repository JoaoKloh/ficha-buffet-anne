import { z } from "zod";
import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { responder } from "@/lib/server/gestao/http";
import { FORMS_OPCOES_EVENTO } from "@/lib/models/evento";

export const dynamic = "force-dynamic";

const Para = z.enum(FORMS_OPCOES_EVENTO, { message: "Informe o formulário das opções (?para=)." });

/** Opções de um formulário de evento (?para=): só os campos que ele usa. */
export async function GET(req: Request) {
  return responder(() => eventoBackend.opcoes(Para.parse(new URL(req.url).searchParams.get("para"))));
}
