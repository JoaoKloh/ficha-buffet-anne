import { backupBackend } from "@/lib/server/gestao/backupBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { RestaurarBackupRequestSchema } from "@/lib/models/backup";

export const dynamic = "force-dynamic";

/** Restaura do backup: cria o que falta e, no modo "todos", volta os diferentes. Nunca apaga. */
export async function POST(req: Request) {
  return responder(async () => backupBackend.restaurar(await corpo(req, RestaurarBackupRequestSchema)));
}
