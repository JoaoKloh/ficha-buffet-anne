// app/api/prato/route.ts
import { NextResponse } from "next/server";
import { pratoBackend, PratoBackendError } from "@/lib/server/pratoBackend";

    export async function POST(req: Request) {
        try {
          const body = await req.json();
          await pratoBackend.criar(body); // Executa sem esperar retorno JSON
      
          return new NextResponse(null, { status: 201 });
        } catch (error) {
          if (error instanceof PratoBackendError) {
            return NextResponse.json({ message: error.message }, { status: error.status });
          }
          return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
        }
    }


export async function PUT(req: Request) {
  try {
    const body = await req.json();
    await pratoBackend.atualizar(body);
    return NextResponse.json({ message: "Atualizado com sucesso." }, { status: 200 });
  } catch (error) {
    if (error instanceof PratoBackendError) {
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

    await pratoBackend.apagar(Number(id));
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof PratoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
  }
}