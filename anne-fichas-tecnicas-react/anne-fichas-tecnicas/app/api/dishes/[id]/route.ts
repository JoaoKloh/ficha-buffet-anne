import { NextRequest, NextResponse } from "next/server";
import { dishRepository } from "@/lib/server/db";
import { UpdateDishInputSchema } from "@/lib/models/dish";
import { requireAuth, UnauthorizedError } from "@/lib/server/auth";

export const dynamic = "force-dynamic";

// UUIDs têm formato fixo — valida antes de tocar o repositório, para rejeitar
// cedo qualquer entrada malformada (ex: tentativas de path traversal).
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function isValidId(id: string): boolean {
  return UUID_RE.test(id);
}

export async function GET(_req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isValidId(id)) {
    return NextResponse.json({ error: "ID inválido." }, { status: 400 });
  }
  const dish = await dishRepository.getById(id);
  if (!dish) {
    return NextResponse.json({ error: "Prato não encontrado." }, { status: 404 });
  }
  return NextResponse.json({ data: dish }, { status: 200 });
}

export async function PATCH(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    requireAuth(req);
    const { id } = await params;
    if (!isValidId(id)) {
      return NextResponse.json({ error: "ID inválido." }, { status: 400 });
    }

    const body = await req.json().catch(() => null);
    const parsed = UpdateDishInputSchema.safeParse(body);
    if (!parsed.success) {
      return NextResponse.json(
        { error: "Dados inválidos", issues: parsed.error.flatten() },
        { status: 400 }
      );
    }

    const updated = await dishRepository.update(id, parsed.data);
    if (!updated) {
      return NextResponse.json({ error: "Prato não encontrado." }, { status: 404 });
    }
    return NextResponse.json({ data: updated }, { status: 200 });
  } catch (err) {
    if (err instanceof UnauthorizedError) {
      return NextResponse.json({ error: err.message }, { status: 401 });
    }
    console.error("[PATCH /api/dishes/:id]", err);
    return NextResponse.json({ error: "Erro ao atualizar prato." }, { status: 500 });
  }
}

export async function DELETE(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    requireAuth(req);
    const { id } = await params;
    if (!isValidId(id)) {
      return NextResponse.json({ error: "ID inválido." }, { status: 400 });
    }

    const removed = await dishRepository.remove(id);
    if (!removed) {
      return NextResponse.json({ error: "Prato não encontrado." }, { status: 404 });
    }
    return new NextResponse(null, { status: 204 });
  } catch (err) {
    if (err instanceof UnauthorizedError) {
      return NextResponse.json({ error: err.message }, { status: 401 });
    }
    console.error("[DELETE /api/dishes/:id]", err);
    return NextResponse.json({ error: "Erro ao excluir prato." }, { status: 500 });
  }
}
