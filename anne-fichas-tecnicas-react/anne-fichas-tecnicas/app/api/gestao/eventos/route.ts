import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreateEventoRequestSchema, FILTROS_EVENTO, UpdateEventoRequestSchema, type FiltroEvento } from "@/lib/models/evento";

export const dynamic = "force-dynamic";

export async function GET(req: Request) {
  const sp = new URL(req.url).searchParams;
  const f = sp.get("filtro") as FiltroEvento | null;
  const filtro = f && FILTROS_EVENTO.some((x) => x[0] === f) ? f : "ativos";
  return responder(() => eventoBackend.listar(filtro, sp.get("busca") ?? ""), 200, "Erro ao carregar eventos.");
}
export async function POST(req: Request) {
  return responder(async () => eventoBackend.criar(await corpo(req, CreateEventoRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => eventoBackend.atualizar(await corpo(req, UpdateEventoRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => eventoBackend.apagar(idQuery(req)));
}
