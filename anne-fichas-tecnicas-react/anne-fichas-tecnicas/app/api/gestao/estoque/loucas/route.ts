import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Disponibilidade de louças e utensílios numa data (padrão: próximo evento que usa louças). */
export async function GET(req: Request) {
  const data = new URL(req.url).searchParams.get("data");
  return responder(() => estoqueBackend.loucas(data && /^\d{4}-\d{2}-\d{2}$/.test(data) ? data : null));
}
