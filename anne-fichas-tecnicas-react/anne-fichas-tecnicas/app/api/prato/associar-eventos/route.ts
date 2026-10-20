// app/api/prato/associar-eventos/route.ts
import { NextResponse } from "next/server";
import { pratoBackend, PratoBackendError } from "@/lib/server/pratoBackend";

export async function POST(req: Request) {
  try {
    const body = await req.json();
    await pratoBackend.associarEventos(body);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (error instanceof PratoBackendError) {
      return NextResponse.json({ message: error.message }, { status: error.status });
    }
    return NextResponse.json({ message: "Erro interno do servidor." }, { status: 500 });
  }
}
