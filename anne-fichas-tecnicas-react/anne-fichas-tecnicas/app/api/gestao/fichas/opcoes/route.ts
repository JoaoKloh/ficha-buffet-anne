import { fichaBackend } from "@/lib/server/gestao/fichaBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

export async function GET() {
  return responder(() => fichaBackend.opcoes());
}
