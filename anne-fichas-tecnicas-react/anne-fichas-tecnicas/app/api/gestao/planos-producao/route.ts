import { planoProducaoBackend } from "@/lib/server/gestao/planoProducaoBackend";
import { corpo, idDe, idQuery, responder } from "@/lib/server/gestao/http";
import { BackendError } from "@/lib/server/gestao/java";
import { UNIDADES_PLANO, type UnidadePlano } from "@/lib/models/dominio";
import { SalvarPlanoRequestSchema } from "@/lib/models/planoProducao";

export const dynamic = "force-dynamic";

/** ?unidade=restaurante|chocolates: lista com KPIs. ?id=: plano com linhas, insumos e compras. */
export async function GET(req: Request) {
  return responder<unknown>(() => {
    const sp = new URL(req.url).searchParams;
    if (sp.get("id")) return planoProducaoBackend.detalhe(idDe(sp.get("id")));
    const un = sp.get("unidade") ?? "restaurante";
    if (!UNIDADES_PLANO.some(([k]) => k === un)) throw new BackendError("Unidade inválida.", 400);
    return planoProducaoBackend.listar(un as UnidadePlano);
  });
}
export async function POST(req: Request) {
  return responder(async () => planoProducaoBackend.criar(await corpo(req, SalvarPlanoRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => planoProducaoBackend.atualizar(idQuery(req), await corpo(req, SalvarPlanoRequestSchema)));
}
/** Exclui o plano; os lotes já registrados continuam em Pré-preparos. */
export async function DELETE(req: Request) {
  return responder(() => planoProducaoBackend.apagar(idQuery(req)));
}
