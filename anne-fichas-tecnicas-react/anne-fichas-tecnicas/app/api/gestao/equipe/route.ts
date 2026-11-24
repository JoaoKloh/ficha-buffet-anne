import { equipeBackend } from "@/lib/server/gestao/equipeBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Todas as pessoas do cadastro da equipe (checklist da escala do evento). */
export async function GET() {
  return responder(() => equipeBackend.listar(), 200, "Erro ao carregar a equipe.");
}
