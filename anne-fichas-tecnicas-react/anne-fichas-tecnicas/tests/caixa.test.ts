import { describe, expect, it } from "vitest";
import {
  ClassificarRequestSchema,
  CreateLancamentoRequestSchema,
  PagamentoRequestSchema,
  RateioRequestSchema,
  SaldosRequestSchema,
  TransferenciaRequestSchema,
} from "@/lib/models/caixa";
import { unCurto, unLabel } from "@/lib/models/dominio";

const ok = {
  descricao: "Ceasa",
  valor: 10.5,
  categoriaId: 3,
  eventoId: null,
  dataCompetencia: "2026-05-01",
  status: "realizado",
  vencimento: null,
  dataPagamento: null,
  unidade: "buffet",
  fornecedor: null,
};

describe("contrato de lançamento", () => {
  it("exige descrição, valor positivo com 2 casas, categoria e unidade de negócio", () => {
    expect(CreateLancamentoRequestSchema.parse(ok).conta).toBe("banco");
    expect(() => CreateLancamentoRequestSchema.parse({ ...ok, valor: 0 })).toThrow();
    expect(() => CreateLancamentoRequestSchema.parse({ ...ok, valor: 1.005 })).toThrow();
    expect(() => CreateLancamentoRequestSchema.parse({ ...ok, descricao: "" })).toThrow("Descreva o lançamento.");
    expect(() => CreateLancamentoRequestSchema.parse({ ...ok, unidade: undefined })).toThrow("Escolha a unidade de negócio.");
    expect(CreateLancamentoRequestSchema.parse({ ...ok, conta: "banco_restaurante" }).conta).toBe("banco_restaurante");
  });

  it("pagamento em parte exige valor e data", () => {
    expect(PagamentoRequestSchema.parse({ valor: 100, data: "2026-05-02", conta: "caixa", forma: "" }).valor).toBe(100);
    expect(() => PagamentoRequestSchema.parse({ valor: 0, data: "2026-05-02", conta: "caixa", forma: "" })).toThrow();
    expect(() => PagamentoRequestSchema.parse({ valor: 10, data: "", conta: "caixa", forma: "" })).toThrow("Informe a data.");
  });

  it("transferência, classificação, saldos e rateio", () => {
    const t = { data: "2026-05-02", responsavel: "", unidadeOrigem: "buffet", unidadeDestino: "restaurante", insumoId: null, item: "Molho", qtd: 2, unid: "kg", valor: 80, categoriaId: 6, obs: "" };
    expect(TransferenciaRequestSchema.parse(t).unidadeDestino).toBe("restaurante");
    expect(() => TransferenciaRequestSchema.parse({ ...t, valor: 0 })).toThrow("O valor deve ser maior que zero.");
    expect(() => ClassificarRequestSchema.parse({ unidade: "chocolates", ids: [] })).toThrow("Nenhum lançamento para classificar.");
    expect(SaldosRequestSchema.parse({ data: "2026-05-01", contas: { banco: 1000, caixa: null } }).contas.banco).toBe(1000);
    expect(() => SaldosRequestSchema.parse({ data: "2026-05-01", contas: { poupanca: 1 } })).toThrow();
    expect(RateioRequestSchema.parse({ buffet: 50, restaurante: 30, chocolates: 20 }).chocolates).toBe(20);
  });

  it("nomes das unidades", () => {
    expect(unCurto("compartilhado")).toBe("Compartilhado");
    expect(unCurto("")).toBe("Sem unidade");
    expect(unLabel("chocolates")).toBe("Chocolates Marie & Anne");
  });
});
