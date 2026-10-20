import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { dishRepository } from "@/lib/server/db";
import { buildSeedDishes } from "@/lib/server/seedData";
import { CreateDishInputSchema } from "@/lib/models/dish";
import { requireAuth, UnauthorizedError } from "@/lib/server/auth";

// Garante que dados sensíveis nunca sejam cacheados por CDN/proxy intermediário.
export const dynamic = "force-dynamic";

export async function GET() {
  try {
    await dishRepository.seedIfEmpty(buildSeedDishes());
    const dishes = await dishRepository.list();
    return NextResponse.json({ data: dishes }, { status: 200 });
  } catch (err) {
    console.error("[GET /api/dishes]", err);
    return NextResponse.json({ error: "Erro ao carregar pratos." }, { status: 500 });
  }
}

export async function POST(req: NextRequest) {
  try {
    requireAuth(req);

    const body = await req.json().catch(() => null);
    const parsed = CreateDishInputSchema.safeParse(body);
    if (!parsed.success) {
      return NextResponse.json(
        { error: "Dados inválidos", issues: parsed.error.flatten() },
        { status: 400 }
      );
    }

    const dish = await dishRepository.create(parsed.data);
    return NextResponse.json({ data: dish }, { status: 201 });
  } catch (err) {
    if (err instanceof UnauthorizedError) {
      return NextResponse.json({ error: err.message }, { status: 401 });
    }
    console.error("[POST /api/dishes]", err);
    return NextResponse.json({ error: "Erro ao criar prato." }, { status: 500 });
  }
}
