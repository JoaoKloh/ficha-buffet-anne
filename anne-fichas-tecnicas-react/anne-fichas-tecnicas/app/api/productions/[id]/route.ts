import { NextRequest, NextResponse } from "next/server";
import { productionRepository } from "@/lib/server/db";
import { UpdateProductionInputSchema } from "@/lib/models/production";
import { requireAuth, UnauthorizedError } from "@/lib/server/auth";

export const dynamic = "force-dynamic";

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
function isValidId(id: string): boolean {
  return UUID_RE.test(id);
}

export async function GET(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isValidId(id)) {
    return NextResponse.json({ error: "ID inválido." }, { status: 400 });
  }
  const production = await productionRepository.getById(id);
  if (!production) {
    return NextResponse.json({ error: "Produção não encontrada." }, { status: 404 });
  }
  return NextResponse.json({ data: production }, { status: 200 });
}

export async function PATCH(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    requireAuth(req);
    const { id } = await params;
    if (!isValidId(id)) {
      return NextResponse.json({ error: "ID inválido." }, { status: 400 });
    }

    const body = await req.json().catch(() => null);
    const parsed = UpdateProductionInputSchema.safeParse(body);
    if (!parsed.success) {
      return NextResponse.json(
        { error: "Dados inválidos", issues: parsed.error.flatten() },
        { status: 400 }
      );
    }

    const updated = await productionRepository.update(id, parsed.data);
    if (!updated) {
      return NextResponse.json({ error: "Produção não encontrada." }, { status: 404 });
    }
    return NextResponse.json({ data: updated }, { status: 200 });
  } catch (err) {
    if (err instanceof UnauthorizedError) {
      return NextResponse.json({ error: err.message }, { status: 401 });
    }
    console.error("[PATCH /api/productions/:id]", err);
    return NextResponse.json({ error: "Erro ao atualizar produção." }, { status: 500 });
  }
}

export async function DELETE(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    requireAuth(req);
    const { id } = await params;
    if (!isValidId(id)) {
      return NextResponse.json({ error: "ID inválido." }, { status: 400 });
    }

    const removed = await productionRepository.remove(id);
    if (!removed) {
      return NextResponse.json({ error: "Produção não encontrada." }, { status: 404 });
    }
    return new NextResponse(null, { status: 204 });
  } catch (err) {
    if (err instanceof UnauthorizedError) {
      return NextResponse.json({ error: err.message }, { status: 401 });
    }
    console.error("[DELETE /api/productions/:id]", err);
    return NextResponse.json({ error: "Erro ao excluir produção." }, { status: 500 });
  }
}
