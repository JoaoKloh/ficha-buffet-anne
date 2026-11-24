import { describe, expect, it } from "vitest";
import { AjustarEquipeRequestSchema } from "@/lib/models/equipe";
import { AssociarEquipeRequestSchema } from "@/lib/models/evento";

describe("contrato do cadastro da equipe", () => {
  it("edição manda só o que mudou e recusa corpo vazio, nome em branco e função fora da lista", () => {
    expect(AjustarEquipeRequestSchema.parse({ diariaPadrao: 180, pix: " ana@pix " })).toEqual({ diariaPadrao: 180, pix: "ana@pix" });
    expect(AjustarEquipeRequestSchema.parse({ contato: "" })).toEqual({ contato: "" });
    expect(() => AjustarEquipeRequestSchema.parse({})).toThrow("Nada a ajustar.");
    expect(() => AjustarEquipeRequestSchema.parse({ nome: "   " })).toThrow("Informe o nome.");
    expect(() => AjustarEquipeRequestSchema.parse({ funcaoPadrao: "Chef" })).toThrow();
    expect(() => AjustarEquipeRequestSchema.parse({ diariaPadrao: 10.005 })).toThrow("O valor deve ter no máximo 2 casas decimais.");
  });

  it("escala em lote aceita a lista inteira (vazia tira todos) e recusa id inválido", () => {
    expect(AssociarEquipeRequestSchema.parse({ equipeIds: [] })).toEqual({ equipeIds: [] });
    expect(AssociarEquipeRequestSchema.parse({ equipeIds: [3, 7] }).equipeIds).toEqual([3, 7]);
    expect(() => AssociarEquipeRequestSchema.parse({ equipeIds: [0] })).toThrow();
  });
});
