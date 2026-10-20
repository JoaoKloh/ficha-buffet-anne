// app/api/ingredients/route.ts
import { NextResponse } from "next/server";
import { ingredientBackend, IngredientBackendError } from "@/lib/server/ingredientBackend";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const ingredients = await ingredientBackend.listarTodos();
    return NextResponse.json(ingredients, { status: 200 });
  } catch (err) {
    if (err instanceof IngredientBackendError) {
      return NextResponse.json({ message: err.message }, { status: err.status });
    }
    return NextResponse.json({ message: "Erro ao carregar ingredientes." }, { status: 500 });
  }
}

export async function POST(req: Request) {
  try {
    const body = await req.json().catch(() => null);
    if (!body) {
      return NextResponse.json({ message: "Corpo da requisição inválido." }, { status: 400 });
    }

    await ingredientBackend.criar(body);
    return new NextResponse(null, { status: 201 });
  } catch (err) {
    if (err instanceof IngredientBackendError) {
      return NextResponse.json({ message: err.message }, { status: err.status });
    }
    return NextResponse.json({ message: "Erro ao criar ingrediente." }, { status: 500 });
  }
}

export async function PUT(req: Request) {
  try {
    const body = await req.json().catch(() => null);
    if (!body || !body.id) {
      return NextResponse.json({ message: "ID e dados do ingrediente são obrigatórios." }, { status: 400 });
    }

    const updated = await ingredientBackend.atualizar(body);
    return NextResponse.json(updated, { status: 200 });
  } catch (err) {
    if (err instanceof IngredientBackendError) {
      return NextResponse.json({ message: err.message }, { status: err.status });
    }
    return NextResponse.json({ message: "Erro ao atualizar ingrediente." }, { status: 500 });
  }
}