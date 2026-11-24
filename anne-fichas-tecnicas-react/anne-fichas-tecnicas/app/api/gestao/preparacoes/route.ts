import { fichaBackend } from "@/lib/server/gestao/fichaBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreatePreparacaoRequestSchema, UpdatePreparacaoRequestSchema } from "@/lib/models/preparacao";

export const dynamic = "force-dynamic";

/** Fichas técnicas com custo e situação calculados. */
export async function GET(req: Request) {
  return responder(() => fichaBackend.listarPreparacoes(new URL(req.url).searchParams.get("busca") ?? ""), 200, "Erro ao carregar as fichas.");
}
export async function POST(req: Request) {
  return responder(async () => fichaBackend.salvarPreparacao(await corpo(req, CreatePreparacaoRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => fichaBackend.salvarPreparacao(await corpo(req, UpdatePreparacaoRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => fichaBackend.apagarPreparacao(idQuery(req)));
}
