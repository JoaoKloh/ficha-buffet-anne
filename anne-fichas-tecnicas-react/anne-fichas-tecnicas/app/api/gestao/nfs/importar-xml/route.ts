import { z } from "zod";
import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { corpo, responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

const LIMITE = 5 * 1024 * 1024;

/** Lê o XML da NF-e enviado como texto e devolve a nota pré-preenchida, sem gravar. */
export async function POST(req: Request) {
  return responder(async () => {
    const { xml } = await corpo(req, z.object({ xml: z.string().min(1, "Arquivo vazio.").max(LIMITE, "Arquivo grande demais para um XML de NF-e.") }));
    return estoqueBackend.importarXml(xml);
  });
}
