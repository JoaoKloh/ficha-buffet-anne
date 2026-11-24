import { caixaBackend } from "@/lib/server/gestao/caixaBackend";
import { corpo, responder } from "@/lib/server/gestao/http";
import { TransferenciaRequestSchema, UpdateTransferenciaRequestSchema } from "@/lib/models/caixa";
import { unidadeDaQuery } from "../unidade";

export const dynamic = "force-dynamic";

/** Transferências entre unidades: lista e saldo de cada unidade no mês. */
export async function GET(req: Request) {
  return responder(() => caixaBackend.transferencias(unidadeDaQuery(req)), 200, "Erro ao carregar as transferências.");
}
export async function POST(req: Request) {
  return responder(async () => caixaBackend.criarTransferencia(await corpo(req, TransferenciaRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => caixaBackend.atualizarTransferencia(await corpo(req, UpdateTransferenciaRequestSchema)));
}
