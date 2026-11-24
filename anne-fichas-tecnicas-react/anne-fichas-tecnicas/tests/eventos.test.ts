import { describe, expect, it } from "vitest";
import { CreateEventoRequestSchema, RegistrarContratoRequestSchema } from "@/lib/models/evento";

describe("contratos do evento", () => {
  it("valida o contrato como o protótipo", () => {
    const ok = { dataAssinatura: "2026-01-01", convidadosContratados: 100, valorPorConvidado: 0, valorTotal: 1000, valorConvidadoExtra: 0, categoriaId: 1, nParcelas: 3, primeiroVencimento: "2026-01-10", intervaloMeses: 1 };
    expect(RegistrarContratoRequestSchema.parse(ok).numero).toBe("");
    expect(() => RegistrarContratoRequestSchema.parse({ ...ok, valorTotal: 0 })).toThrow();
  });
  it("evento exige cliente existente ou novo", () => {
    const base = { nome: "X", tipo: "casamento", status: "lead", dataEvento: "", convidadosPrevistos: null, margem: 5, clienteId: null };
    expect(() => CreateEventoRequestSchema.parse(base)).toThrow("Escolha o cliente ou cadastre um novo.");
    expect(CreateEventoRequestSchema.parse({ ...base, novoCliente: { nome: "Ana" } }).novoCliente?.nome).toBe("Ana");
  });
});
