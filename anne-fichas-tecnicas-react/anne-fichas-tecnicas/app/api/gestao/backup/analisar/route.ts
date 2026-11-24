import { backupBackend } from "@/lib/server/gestao/backupBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { AnalisarBackupRequestSchema } from "@/lib/models/backup";

export const dynamic = "force-dynamic";

/** Lê e confere o arquivo de backup contra o sistema, sem gravar nada. */
export async function POST(req: Request) {
  return responder(async () => backupBackend.analisar((await corpo(req, AnalisarBackupRequestSchema)).arquivo));
}
