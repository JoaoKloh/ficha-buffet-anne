// app/api/ingredients/[id]/route.ts
import { NextResponse } from "next/server";
import { ingredientBackend, IngredientBackendError } from "@/lib/server/ingredientBackend";

export const dynamic = "force-dynamic";

export async function DELETE(
  _req: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id: rawId } = await params;
    const id = Number(rawId);

    if (!id || isNaN(id)) {
      return NextResponse.json({ message: "ID inválido." }, { status: 400 });
    }

    await ingredientBackend.apagar(id);
    return new NextResponse(null, { status: 204 });
  } catch (err) {
    if (err instanceof IngredientBackendError) {
      return NextResponse.json({ message: err.message }, { status: err.status });
    }
    return NextResponse.json({ message: "Erro ao excluir ingrediente." }, { status: 500 });
  }
}