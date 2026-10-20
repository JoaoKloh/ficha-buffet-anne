// lib/server/producaoBackend.ts
import { cookies } from "next/headers";
import type {
  ProducaoResponseDTO,
  CreateProducaoRequestDTO,
  UpdateProducaoRequestDTO,
} from "@/lib/models/producao";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export class ProducaoBackendError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "ProducaoBackendError";
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
    throw new ProducaoBackendError(
      payload?.message ?? `Erro na requisição ao backend (${res.status})`,
      res.status
    );
  }

  return res;
}

export const producaoBackend = {
  async listarTodos(): Promise<ProducaoResponseDTO[]> {
    const res = await call("/producao/retornarTodos");
    return res.json();
  },

  // Retorna void porque o Spring Boot responde 204 No Content
  async criar(body: CreateProducaoRequestDTO): Promise<void> {
    await call("/producao/criar", { method: "POST", body: JSON.stringify(body) });
  },

  async atualizar(body: UpdateProducaoRequestDTO): Promise<void> {
    await call("/producao/atualizar", { method: "PUT", body: JSON.stringify(body) });
  },

  async apagar(id: number): Promise<void> {
    await call(`/producao/apagar/${id}`, { method: "DELETE" });
  },
};
