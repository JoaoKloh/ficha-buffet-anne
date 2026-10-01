// app/api/lancamentos/route.ts
import { NextResponse } from "next/server";
import { lancamentoBackend, LancamentoBackendError } from "@/lib/server/lancamentoBackend";

export const dynamic = "force-dynamic";

// Respostas com corpo vão no envelope { data }, que o apiClient desembrulha
// (ver lib/api/client.ts). Sem ele, o apiClient pegaria o campo `data` do
// próprio lançamento (a data "AAAA-MM-DD") no lugar do objeto inteiro.

export async function GET() {
  try {
    const lancamentos = await lancamentoBackend.listarTodos();
    return NextResponse.json({ data: lancamentos }, { status: 200 });
  } catch (error) {
    if (error instanceof LancamentoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro ao carregar lançamentos." }, { status: 500 });
  }
}

export async function POST(req: Request) {
  try {
    const body = await req.json().catch(() => null);
    if (!body) {
      return NextResponse.json({ message: "Corpo da requisição inválido." }, { status: 400 });
    }

    const novo = await lancamentoBackend.criar(body);
    return NextResponse.json({ data: novo }, { status: 201 });
  } catch (error) {
    if (error instanceof LancamentoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
  }
}

export async function PUT(req: Request) {
  try {
    const body = await req.json().catch(() => null);
    if (!body) {
      return NextResponse.json({ message: "Corpo da requisição inválido." }, { status: 400 });
    }

    const atualizado = await lancamentoBackend.atualizar(body);
    return NextResponse.json({ data: atualizado }, { status: 200 });
  } catch (error) {
    if (error instanceof LancamentoBackendError) {
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

    await lancamentoBackend.apagar(id);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof LancamentoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
  }
}
