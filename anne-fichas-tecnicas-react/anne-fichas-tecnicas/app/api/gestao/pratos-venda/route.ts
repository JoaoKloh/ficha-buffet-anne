import { fichaBackend } from "@/lib/server/gestao/fichaBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreatePratoVendaRequestSchema, UpdatePratoVendaRequestSchema } from "@/lib/models/pratoVenda";

export const dynamic = "force-dynamic";

/** Carta do restaurante: KPIs de CMV e pratos com custo calculado. */
export async function GET(req: Request) {
  return responder(() => fichaBackend.telaPratosVenda(new URL(req.url).searchParams.get("busca") ?? ""), 200, "Erro ao carregar os pratos.");
}
export async function POST(req: Request) {
  return responder(async () => fichaBackend.salvarPratoVenda(await corpo(req, CreatePratoVendaRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => fichaBackend.salvarPratoVenda(await corpo(req, UpdatePratoVendaRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => fichaBackend.apagarPratoVenda(idQuery(req)));
}
