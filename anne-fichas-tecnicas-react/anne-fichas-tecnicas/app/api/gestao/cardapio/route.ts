import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreateItemCardapioRequestSchema, UpdateItemCardapioRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";

export async function POST(req: Request) {
  return responder(async () => eventoBackend.salvarItemCardapio(await corpo(req, CreateItemCardapioRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => eventoBackend.salvarItemCardapio(await corpo(req, UpdateItemCardapioRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => eventoBackend.apagarItemCardapio(idQuery(req)));
}
