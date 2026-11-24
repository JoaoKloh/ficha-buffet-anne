import { describe, expect, it } from "vitest";
import { CreatePreparacaoRequestSchema } from "@/lib/models/preparacao";
import { CreatePratoVendaRequestSchema } from "@/lib/models/pratoVenda";

describe("contrato do prato do restaurante", () => {
  it("exige ao menos uma ficha ou insumo", () => {
    expect(() => CreatePratoVendaRequestSchema.parse({ nome: "X" })).toThrow("Ligue pelo menos uma ficha técnica ou insumo ao prato.");
  });
});

describe("contrato da preparação", () => {
  it("cada item é insumo ou sub-receita, nunca os dois", () => {
    const base = { nome: "A", categoria: "volante", praca: "Salgados", rendimento: 1 };
    expect(() => CreatePreparacaoRequestSchema.parse({ ...base, itens: [{ insumoId: 1, subId: 2, qtd: 1 }] })).toThrow();
    expect(CreatePreparacaoRequestSchema.parse({ ...base, itens: [{ insumoId: 1, qtd: 1 }] }).itens[0]!.fc).toBe(1);
    expect(() => CreatePreparacaoRequestSchema.parse({ ...base, rendimento: 0 })).toThrow("Informe quanto a receita rende.");
  });
});
