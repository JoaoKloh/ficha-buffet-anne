import { describe, expect, it } from "vitest";
import { CreateClienteRequestSchema, UpdateClienteRequestSchema } from "@/lib/models/cliente";

describe("contrato de cliente", () => {
  it("exige o nome e aplica os padrões do formulário", () => {
    expect(() => CreateClienteRequestSchema.parse({ nome: "  " })).toThrow("Informe o nome do cliente.");
    expect(CreateClienteRequestSchema.parse({ nome: "Isabela" })).toEqual({ nome: "Isabela", tipo: "PF", documento: "", telefone: "", email: "", origem: "" });
  });

  it("aceita só as origens do protótipo", () => {
    expect(() => CreateClienteRequestSchema.parse({ nome: "A", origem: "tiktok" })).toThrow();
    expect(CreateClienteRequestSchema.parse({ nome: "A", origem: "indicacao" }).origem).toBe("indicacao");
  });

  it("atualizar exige o id", () => {
    expect(() => UpdateClienteRequestSchema.parse({ nome: "A" })).toThrow();
  });
});
