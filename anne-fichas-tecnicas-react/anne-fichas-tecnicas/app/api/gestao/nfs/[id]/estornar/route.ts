import { estoqueBackend } from "@/lib/server/gestao/estoqueBackend";
import { idDe, responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

export async function POST(_req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => {
    await estoqueBackend.estornarNf(idDe(id));
    return { ok: true };
  });
}
