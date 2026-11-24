import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { idDe, responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Ficha do item no estoque: saldo, reservas, movimentos e fichas que o usam. */
export async function GET(_req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(() => estoqueBackend.item(idDe(id)));
}
