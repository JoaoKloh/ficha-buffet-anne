import { painelBackend } from "@/lib/server/gestao/painelBackend";
import { responder } from "@/lib/server/gestao/http";

export const dynamic = "force-dynamic";

/** Resumo do Painel: KPIs, alertas, próximos eventos e séries dos gráficos, já calculados. */
export async function GET() {
  return responder(() => painelBackend.resumo(), 200, "Erro ao carregar o painel.");
}
