import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { MarcarPedidoRequestSchema } from "@/lib/models/estoque";

export const dynamic = "force-dynamic";

/** Marca ou desmarca "pedido feito" de um item (sai dos totais até a mercadoria entrar). */
export async function PUT(req: Request) {
  return responder(async () => {
    await estoqueBackend.marcarPedido(await corpo(req, MarcarPedidoRequestSchema));
    return { ok: true };
  });
}
