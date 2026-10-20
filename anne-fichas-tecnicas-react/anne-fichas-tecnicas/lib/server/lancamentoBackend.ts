// lib/server/lancamentoBackend.ts
import { cookies } from "next/headers";
import type {
  LancamentoResponseDTO,
  CriarLancamentoDTO,
  AtualizarLancamentoDTO,
} from "@/lib/models/financeiro";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export class LancamentoBackendError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "LancamentoBackendError";
    this.status = status;
  }
}

// 401/403 vêm do Spring Security sem corpo JSON — sem isto o usuário veria só o código HTTP.
function mensagemPadrao(status: number): string {
  if (status === 401) return "Sua sessão expirou. Faça login novamente.";
  if (status === 403) return "Você não tem permissão para gerenciar lançamentos financeiros.";
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
    throw new LancamentoBackendError(payload?.message ?? mensagemPadrao(res.status), res.status);
  }

  return res;
}

export const lancamentoBackend = {
  async listarTodos(): Promise<LancamentoResponseDTO[]> {
    const res = await call("/lancamentos/retornarTodos");
    return res.json();
  },

  // Diferente de /producao/criar: o Spring Boot responde 201 com o lançamento criado (com id).
  async criar(body: CriarLancamentoDTO): Promise<LancamentoResponseDTO> {
    const res = await call("/lancamentos/criar", { method: "POST", body: JSON.stringify(body) });
    return res.json();
  },

  async atualizar(body: AtualizarLancamentoDTO): Promise<LancamentoResponseDTO> {
    const res = await call("/lancamentos/atualizar", { method: "PUT", body: JSON.stringify(body) });
    return res.json();
  },

  async apagar(id: number): Promise<void> {
    await call(`/lancamentos/apagar/${id}`, { method: "DELETE" });
  },
};
