// lib/server/ingredientBackend.ts
import { cookies } from "next/headers";
import type {
  IngredienteResponseDTO,
  CreateIngredienteRequestDTO,
  UpdateIngredienteRequestDto,
} from "@/lib/models/ingredient";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export class IngredientBackendError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "IngredientBackendError";
    this.status = status;
  }
}

async function call(path: string, init?: RequestInit): Promise<Response> {
  const cookieStore = await cookies();
  const token = cookieStore.get("accessToken")?.value;

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Cookie: token ? `accessToken=${token}` : "",
      ...(init?.headers ?? {}),
    },
    cache: "no-store",
  });

  if (!res.ok) {
    const payload = await res.json().catch(() => null);
    throw new IngredientBackendError(
      payload?.message ?? `Erro no backend (${res.status})`,
      res.status
    );
  }

  return res;
}

export const ingredientBackend = {
  async listarTodos(): Promise<IngredienteResponseDTO[]> {
    const res = await call("/ingredientes/retornarTodos");
    return res.json();
  },

  // Retorna void porque o Spring Boot responde 204 No Content (sem o
  // ingrediente criado, sem id) — mesmo contrato de pratoBackend.criar/
  // producaoBackend.criar.
  async criar(body: CreateIngredienteRequestDTO): Promise<void> {
    await call("/ingredientes/criar", { method: "POST", body: JSON.stringify(body) });
  },

  async atualizar(body: UpdateIngredienteRequestDto & { id: number }): Promise<IngredienteResponseDTO> {
    const res = await call("/ingredientes/atualizar", {
      method: "PUT",
      body: JSON.stringify(body),
    });
    // Se o backend retornar 204 No Content, cai no fallback do body
    return res.status === 204 ? (body as unknown as IngredienteResponseDTO) : res.json();
  },

  async apagar(id: number): Promise<void> {
    await call(`/ingredientes/apagar/${id}`, { method: "DELETE" });
  },
};