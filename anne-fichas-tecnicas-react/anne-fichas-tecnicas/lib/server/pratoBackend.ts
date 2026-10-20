// lib/server/pratoBackend.ts
import { cookies } from "next/headers";
import type {
  PratoDetalhadoResponseDTO,
  UpdatePratoRequest,
  AssociationPratoProducaoRequestDTO,
} from "@/lib/models/prato";
import type { CreateDishInput } from "@/lib/models/dish";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export class PratoBackendError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "PratoBackendError";
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
    throw new PratoBackendError(
      payload?.message ?? `Erro na requisição ao backend (${res.status})`,
      res.status
    );
  }

  return res;
}

export const pratoBackend = {
  async listarTodos(): Promise<PratoDetalhadoResponseDTO[]> {
    const res = await call("/prato/retornarTodos");
    return res.json();
  },

  // Retorna void porque o Spring Boot responde 204 No Content
  async criar(body: CreateDishInput): Promise<void> {
    await call("/prato/criar", { method: "POST", body: JSON.stringify(body) });
  },

  async atualizar(body: UpdatePratoRequest): Promise<void> {
    await call("/prato/atualizar", { method: "PUT", body: JSON.stringify(body) });
  },

  async apagar(id: number): Promise<void> {
    await call(`/prato/apagar/${id}`, { method: "DELETE" });
  },

  async associarEventos(body: AssociationPratoProducaoRequestDTO): Promise<void> {
    await call("/prato/associarEventos", { method: "POST", body: JSON.stringify(body) });
  },

  // Espelha AutenticacaoController#login (POST /auth/login). Devolve a
  // Response crua (não json): o que importa é o header Set-Cookie
  // "accessToken" devolvido pelo Spring, repassado ao browser pela rota
  // /api/auth/login na própria origem do Next.js — ver app/api/auth/login/route.ts.
  async login(credentials: { username: string; password: string }): Promise<Response> {
    return call("/auth/login", { method: "POST", body: JSON.stringify(credentials) });
  },
};