import { backupBackend } from "@/lib/server/gestao/backupBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Quantos registros há por coleção e quando foi o último backup completo. */
export async function GET() {
  return responder(() => backupBackend.resumo(), 200, "Erro ao carregar o backup.");
}

/** Registra que um backup completo (.json) acabou de ser salvo. */
export async function POST() {
  return responder(() => backupBackend.registrar(), 201);
}
