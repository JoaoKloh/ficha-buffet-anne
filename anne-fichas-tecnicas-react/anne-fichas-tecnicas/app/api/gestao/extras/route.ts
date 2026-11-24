import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idQuery, responder } from "@/lib/server/gestao/http";
import { CreateExtraRequestSchema, UpdateExtraRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";

export async function POST(req: Request) {
  return responder(async () => eventoBackend.salvarExtra(await corpo(req, CreateExtraRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => eventoBackend.salvarExtra(await corpo(req, UpdateExtraRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => eventoBackend.apagarExtra(idQuery(req)));
}
