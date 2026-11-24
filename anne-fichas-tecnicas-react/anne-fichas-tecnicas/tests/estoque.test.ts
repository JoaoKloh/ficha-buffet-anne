import { describe, expect, it } from "vitest";
import { conferirLinha, gerarParcelas, qtdConvertida, totalNf } from "@/lib/utils/nf";
import { MovimentarLoteRequestSchema, normalizarCompras, normalizarConferencia, normalizarPerdas, normalizarValidades } from "@/lib/models/estoque";

describe("formulário da nota fiscal", () => {
  it("total = soma dos itens + acréscimo, conversão por embalagem e conferência da linha", () => {
    expect(totalNf([{ valor: 10.1 }, { valor: 5.2 }], 1.33)).toBe(16.63);
    expect(qtdConvertida(3, "compra", 0.2)).toBe(0.6);
    expect(qtdConvertida(3, "estoque", 0.2)).toBe(3);
    expect(conferirLinha(2, 10.5, 21)).toEqual({ esperado: 21, ok: true });
    expect(conferirLinha(2, 10.5, 22)!.ok).toBe(false);
  });

  it("divide parcelas por dias ou mês a mês", () => {
    expect(gerarParcelas(100, 3, "2026-01-31", "m", false).map((p) => [p.venc, p.valor])).toEqual([["2026-01-31", 33.33], ["2026-02-28", 33.33], ["2026-03-31", 33.34]]);
    expect(gerarParcelas(100, 2, "2026-01-01", "30", false)[1]!.venc).toBe("2026-01-31");
  });
});

describe("estoque integrado", () => {
  it("resposta nula ou com campos nulos vira estrutura completa (as abas nunca escondem blocos)", () => {
    const c = normalizarCompras(null, 15);
    expect(c).toEqual({ horizonte: 15, kpis: { itens: 0, fornecedores: 0, valor: 0, semPreco: false, pedidos: 0, eventos: 0 }, fornecedores: [] });
    const comNulos = normalizarCompras({ kpis: null as never, fornecedores: [{ fornecedor: "", valor: null as never, itens: [{ insumoId: 1, eventos: null } as never] }] }, 7);
    expect(comNulos.fornecedores[0]!.fornecedor).toBe("Sem fornecedor definido");
    expect(comNulos.fornecedores[0]!.itens[0]!.eventos).toEqual([]);
    expect(comNulos.fornecedores[0]!.itens[0]!.saldo).toBe(0);
    expect(normalizarValidades(undefined)).toEqual({ kpis: { vencidos: 0, vencem: 0, valorRisco: 0, semValidade: 0 }, itens: [] });
    const p = normalizarPerdas({ kpis: { pctCompras: null, maiorCausa: "" } as never }, "2026-11");
    expect(p.mes).toBe("2026-11");
    expect(p.kpis.pctCompras).toBeNull();
    expect(p.kpis.maiorCausa).toBeNull();
    expect(p.porGrupo).toEqual([]);
    expect(normalizarConferencia({ porOrigem: null as never })).toEqual({ porOrigem: [], pontos: [] });
  });

  it("uso de lote em evento exige o evento", () => {
    const base = { tipo: "uso", qtd: 1, data: "2026-11-19", para: "evento" } as const;
    expect(MovimentarLoteRequestSchema.safeParse(base).success).toBe(false);
    expect(MovimentarLoteRequestSchema.parse({ ...base, eventoId: 3 }).eventoId).toBe(3);
    expect(MovimentarLoteRequestSchema.parse({ ...base, para: "restaurante" }).eventoId).toBeNull();
  });
});
