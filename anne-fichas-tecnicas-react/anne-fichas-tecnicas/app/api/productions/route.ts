import { NextRequest, NextResponse } from "next/server";
import { productionRepository } from "@/lib/server/db";
import { CreateProductionInputSchema } from "@/lib/models/production";
import { requireAuth, UnauthorizedError } from "@/lib/server/auth";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const productions = await productionRepository.list();
    return NextResponse.json({ data: productions }, { status: 200 });
  } catch (err) {
    console.error("[GET /api/productions]", err);
    return NextResponse.json({ error: "Erro ao carregar produções." }, { status: 500 });
  }
}

export async function POST(req: NextRequest) {
  try {
    requireAuth(req);

    const body = await req.json().catch(() => null);
    const parsed = CreateProductionInputSchema.safeParse(body);
    if (!parsed.success) {
      return NextResponse.json(
        { error: "Dados inválidos", issues: parsed.error.flatten() },
        { status: 400 }
      );
    }

    const production = await productionRepository.create(parsed.data);
    return NextResponse.json({ data: production }, { status: 201 });
  } catch (err) {
    if (err instanceof UnauthorizedError) {
      return NextResponse.json({ error: err.message }, { status: 401 });
    }
    console.error("[POST /api/productions]", err);
    return NextResponse.json({ error: "Erro ao criar produção." }, { status: 500 });
  }
}
