import { backupBackend } from "@/lib/server/gestao/backupBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Backup completo: a cópia exata, que permite restaurar. */
export async function GET() {
  return responder(() => backupBackend.exportar(), 200, "Erro ao gerar o backup.");
}
