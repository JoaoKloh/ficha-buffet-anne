import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { InventarioRequestSchema } from "@/lib/models/estoque";

export const dynamic = "force-dynamic";

export async function POST(req: Request) {
  return responder(async () => ({ lancados: await estoqueBackend.inventario(await corpo(req, InventarioRequestSchema)) }), 201);
}
