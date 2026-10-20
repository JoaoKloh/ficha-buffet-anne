// app/api/producao/route.ts
import { NextResponse } from "next/server";
import { producaoBackend, ProducaoBackendError } from "@/lib/server/producaoBackend";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const producoes = await producaoBackend.listarTodos();
    return NextResponse.json(producoes, { status: 200 });
  } catch (error) {
    if (error instanceof ProducaoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro ao carregar produções." }, { status: 500 });
  }
}

export async function POST(req: Request) {
  try {
    const body = await req.json();
    await producaoBackend.criar(body); // Executa sem esperar retorno JSON

    return new NextResponse(null, { status: 201 });
  } catch (error) {
    if (error instanceof ProducaoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
  }
}

export async function PUT(req: Request) {
  try {
    const body = await req.json();
    await producaoBackend.atualizar(body);
    return NextResponse.json({ message: "Atualizado com sucesso." }, { status: 200 });
  } catch (error) {
    if (error instanceof ProducaoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
  }
}

export async function DELETE(req: Request) {
  try {
    const { searchParams } = new URL(req.url);
    const id = searchParams.get("id");

    if (!id) {
      return NextResponse.json({ message: "ID obrigatório." }, { status: 400 });
    }

    await producaoBackend.apagar(Number(id));
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof ProducaoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
  }
}
