// app/api/orcamento/route.ts
import { NextResponse } from "next/server";
import { orcamentoBackend, OrcamentoBackendError } from "@/lib/server/orcamentoBackend";

export const dynamic = "force-dynamic";

// Respostas com corpo vão no envelope { data }, que o apiClient desembrulha
// (ver lib/api/client.ts), como em /api/lancamentos.

export async function GET() {
  try {
    const orcamentos = await orcamentoBackend.listarTodos();
    return NextResponse.json({ data: orcamentos }, { status: 200 });
  } catch (error) {
    if (error instanceof OrcamentoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro ao carregar orçamentos." }, { status: 500 });
  }
}

export async function POST(req: Request) {
  try {
    const body = await req.json().catch(() => null);
    if (!body) {
      return NextResponse.json({ message: "Corpo da requisição inválido." }, { status: 400 });
    }

    const novo = await orcamentoBackend.criar(body);
    return NextResponse.json({ data: novo }, { status: 201 });
  } catch (error) {
    if (error instanceof OrcamentoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
  }
}

export async function DELETE(req: Request) {
  try {
    const { searchParams } = new URL(req.url);
    const id = Number(searchParams.get("id"));

    if (!Number.isInteger(id) || id <= 0) {
      return NextResponse.json({ message: "ID obrigatório." }, { status: 400 });
    }

    await orcamentoBackend.apagar(id);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof OrcamentoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
  }
}
