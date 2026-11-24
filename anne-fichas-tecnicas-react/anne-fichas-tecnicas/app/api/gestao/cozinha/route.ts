import { cozinhaBackend } from "@/lib/server/gestao/cozinhaBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Tudo que a cozinha vê, sem valores financeiros: projeções dos eventos, execuções e tarefas. */
export async function GET() {
  return responder(() => cozinhaBackend.tela(), 200, "Erro ao carregar a cozinha.");
}
