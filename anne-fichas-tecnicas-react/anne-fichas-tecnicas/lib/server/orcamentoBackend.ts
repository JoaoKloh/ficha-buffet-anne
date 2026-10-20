// lib/server/orcamentoBackend.ts
import { cookies } from "next/headers";
import type { CreateOrcamentoRequest, OrcamentoDetalhadoDTO } from "@/lib/models/orcamento";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export class OrcamentoBackendError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "OrcamentoBackendError";
    this.status = status;
  }
}

// 401/403 vêm do Spring Security sem corpo JSON — sem isto o usuário veria só o código HTTP.
function mensagemPadrao(status: number): string {
  if (status === 401) return "Sua sessão expirou. Faça login novamente.";
  if (status === 403) return "Você não tem permissão para gerenciar orçamentos.";
  return `Erro na requisição ao backend (${status})`;
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
    throw new OrcamentoBackendError(payload?.message ?? mensagemPadrao(res.status), res.status);
  }

  return res;
}

export const orcamentoBackend = {
  async listarTodos(): Promise<OrcamentoDetalhadoDTO[]> {
    const res = await call("/orcamento/retornarTodos");
    return res.json();
  },

  // O Spring Boot responde 201 com o orçamento criado (com id).
  async criar(body: CreateOrcamentoRequest): Promise<OrcamentoDetalhadoDTO> {
    const res = await call("/orcamento/criar", { method: "POST", body: JSON.stringify(body) });
    return res.json();
  },

  async apagar(id: number): Promise<void> {
    await call(`/orcamento/apagar/${id}`, { method: "DELETE" });
  },
};
