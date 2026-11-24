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
