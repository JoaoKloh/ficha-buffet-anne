import { eventoBackend } from "@/lib/server/gestao/eventoBackend";
import { corpo, idDe, idQuery, responder } from "@/lib/server/gestao/http";
import { SalvarPessoaEquipeRequestSchema } from "@/lib/models/evento";

export const dynamic = "force-dynamic";
type Ctx = { params: Promise<{ id: string }> };

/** Linha da escala (?pessoa=). */
const pessoaDe = (req: Request): number => idQuery(req, "pessoa");

/** Escala de equipe do evento: escalar, editar e tirar da escala. */
export async function POST(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => eventoBackend.criarPessoaEquipe(idDe(id), await corpo(req, SalvarPessoaEquipeRequestSchema)), 201);
}
export async function PUT(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(async () => eventoBackend.atualizarPessoaEquipe(idDe(id), pessoaDe(req), await corpo(req, SalvarPessoaEquipeRequestSchema)));
}
export async function DELETE(req: Request, { params }: Ctx) {
  const { id } = await params;
  return responder(() => eventoBackend.apagarPessoaEquipe(idDe(id), pessoaDe(req)));
}
