import { clienteBackend } from "@/lib/server/gestao/clienteBackend";
import { corpo, idDe, idQuery, responder } from "@/lib/server/gestao/http";
import { CreateClienteRequestSchema, UpdateClienteRequestSchema } from "@/lib/models/cliente";

export const dynamic = "force-dynamic";

/** ?id= devolve um cliente (com o resumo de eventos); sem id, a lista filtrada por ?busca=. */
export async function GET(req: Request) {
  return responder<unknown>(() => {
    const sp = new URL(req.url).searchParams;
    if (sp.get("id")) return clienteBackend.detalhar(idDe(sp.get("id")));
    return clienteBackend.listar(sp.get("busca") ?? "");
  }, 200, "Erro ao carregar clientes.");
}
export async function POST(req: Request) {
  return responder(async () => clienteBackend.criar(await corpo(req, CreateClienteRequestSchema)), 201);
}
export async function PUT(req: Request) {
  return responder(async () => clienteBackend.atualizar(await corpo(req, UpdateClienteRequestSchema)));
}
export async function DELETE(req: Request) {
  return responder(() => clienteBackend.apagar(idQuery(req)));
}
