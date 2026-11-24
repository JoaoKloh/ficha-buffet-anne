import { describe, expect, it } from "vitest";
import { AjustarEtapaRequestSchema, SalvarOrcamentoRequestSchema, SalvarPessoaEquipeRequestSchema } from "@/lib/models/evento";
import { CreateMovimentoRequestSchema, MarcarPedidoRequestSchema } from "@/lib/models/estoque";
import { AcaoOrdemRequestSchema, AutorizarLiberacaoRequestSchema, CriarOcorrenciaRequestSchema, PlanoOrdemRequestSchema } from "@/lib/models/cozinha";
import { AdicionarItensPlanoRequestSchema, CalcularCartaRequestSchema, SalvarPlanoRequestSchema } from "@/lib/models/planoProducao";
import { CreateCategoriaRequestSchema } from "@/lib/models/caixa";
import { origemMovimento } from "@/lib/utils/estoque";
import { situacaoOrdem, statusProducao } from "@/lib/utils/producao";
import { documentoDre } from "@/components/gestao/dre/imprimir";
import { documentoPlano } from "@/components/gestao/producao/imprimir";
import { nomeArquivo } from "@/lib/utils/documento";
import { addDays, hoje } from "@/lib/utils/datas";
import { pratoProjecao, projecao } from "./fixtures";

describe("evento como projeto", () => {
  it("escala: nome obrigatório, horário HH:MM e diária com 2 casas", () => {
    const ok = { nome: "Ana", funcao: "Garçom", chegada: "18:00", saida: "", valor: 150 };
    expect(SalvarPessoaEquipeRequestSchema.parse(ok).pix).toBe("");
    expect(() => SalvarPessoaEquipeRequestSchema.parse({ ...ok, nome: " " })).toThrow("Informe o nome.");
    expect(() => SalvarPessoaEquipeRequestSchema.parse({ ...ok, chegada: "18h" })).toThrow("Horário inválido.");
    expect(() => SalvarPessoaEquipeRequestSchema.parse({ ...ok, funcao: "Chef" })).toThrow();
  });
  it("orçamento aceita equipe nula (vem da escala) e etapa exige algo a ajustar", () => {
    expect(SalvarOrcamentoRequestSchema.parse({ equipe: null, transporte: 300, outros: 0 }).equipe).toBeNull();
    expect(() => AjustarEtapaRequestSchema.parse({ etapa: "degustacao" })).toThrow("Nada a ajustar.");
    expect(AjustarEtapaRequestSchema.parse({ etapa: "degustacao", dispensada: true }).dispensada).toBe(true);
  });
  it("categoria guarda como conta no evento (null = automático)", () => {
    expect(CreateCategoriaRequestSchema.parse({ nome: "Frete", grupo: "custo_direto" }).custoEvento).toBeNull();
    expect(CreateCategoriaRequestSchema.parse({ nome: "Frete", grupo: "custo_direto", custoEvento: "transporte" }).custoEvento).toBe("transporte");
  });
});

describe("estoque integrado", () => {
  const base = { insumoId: 1, data: "2026-11-18", qtd: 2 };
  it("consumo exige quem usou e não aceita comprador igual a quem usou", () => {
    expect(() => CreateMovimentoRequestSchema.parse({ ...base, tipo: "consumo" })).toThrow("Escolha a unidade que usou.");
    expect(() => CreateMovimentoRequestSchema.parse({ ...base, tipo: "consumo", unidadeUso: "restaurante", compradoPor: "restaurante" })).toThrow("mesma unidade");
    expect(CreateMovimentoRequestSchema.parse({ ...base, tipo: "consumo", unidadeUso: "restaurante", compradoPor: "buffet" }).compradoPor).toBe("buffet");
  });
  it("entrada com validade; pedido de compra", () => {
    expect(CreateMovimentoRequestSchema.parse({ ...base, tipo: "entrada", validade: "2026-11-25" }).validade).toBe("2026-11-25");
    expect(() => CreateMovimentoRequestSchema.parse({ ...base, tipo: "entrada", validade: "25/11" })).toThrow("Validade inválida.");
    expect(MarcarPedidoRequestSchema.parse({ insumoId: 3, feito: true }).feito).toBe(true);
  });
  it("rótulo de origem do movimento", () => {
    expect(origemMovimento({ tipo: "consumo", origem: "consumo", unidadeUso: "chocolates", eventoId: null })).toBe("Consumo · Chocolates");
    expect(origemMovimento({ tipo: "baixa", origem: "ordem", virtual: true, eventoId: 3 })).toBe("Ordem de produção");
    expect(origemMovimento({ tipo: "entrada", origem: "nf", eventoId: null })).toBe("Compra recebida (NF)");
  });
});

describe("ordens de produção", () => {
  it("ações validadas por tipo", () => {
    expect(() => AcaoOrdemRequestSchema.parse({ acao: "iniciar", eventoId: 1, k: "m_1", quem: "" })).toThrow("Informe seu nome.");
    expect(() => AcaoOrdemRequestSchema.parse({ acao: "bloquear", eventoId: 1, k: "m_1", quem: "Lu", motivo: "insumo", texto: "" })).toThrow("Descreva o impedimento.");
    expect(AcaoOrdemRequestSchema.parse({ acao: "concluir", eventoId: 1, k: "m_1", quem: "Lu", qtd: 10 })).toMatchObject({ perda: 0, obs: "" });
    expect(PlanoOrdemRequestSchema.parse({ eventoId: 1, k: "s_2", dn: 6, setor: "fria" }).hora).toBe("");
    expect(AutorizarLiberacaoRequestSchema.parse({ eventoId: 1, quem: "Chef" }).pendencias).toBe("");
    expect(() => CriarOcorrenciaRequestSchema.parse({ eventoId: 1, tipo: "Atraso", quem: "Lu", texto: "" })).toThrow("Descreva o que aconteceu.");
  });
  it("situação: atrasada vence o status", () => {
    expect(situacaoOrdem({ status: "em_producao", atrasada: true })).toEqual(["bad", "Atrasada · Em produção"]);
    expect(situacaoOrdem({ status: "concluida", atrasada: false })).toEqual(["good", "Concluída"]);
  });
  it("status do prato na lista de produção pelo prazo", () => {
    const t = hoje();
    const pj = projecao({ data: addDays(t, 2) });
    const prato = pratoProjecao({ cid: 1, nome: "Quiche", dnf: 5 });
    expect(statusProducao(pj, prato, {})[1]).toBe("Atrasada");
    expect(statusProducao(pj, prato, { inicio: "08:00" })[1]).toBe("Em produção");
    expect(statusProducao(pj, prato, { produzido: 10 })[1]).toBe("Concluída");
    expect(statusProducao(pj, { ...prato, dnf: 1 }, {})[1]).toBe("Pendente");
  });
});

describe("planos de produção", () => {
  it("datas, itens e carta", () => {
    const ok = { titulo: "Semana", unidade: "restaurante", dataInicio: "2026-11-18", dataFim: "2026-11-24" };
    expect(SalvarPlanoRequestSchema.parse(ok).status).toBe("planejado");
    expect(() => SalvarPlanoRequestSchema.parse({ ...ok, dataFim: "2026-11-01" })).toThrow("antes do início");
    expect(() => AdicionarItensPlanoRequestSchema.parse({ itens: [] })).toThrow("Marque ao menos um item.");
    expect(() => CalcularCartaRequestSchema.parse({ previsao: { "3": 0 } })).toThrow("Informe ao menos um prato previsto.");
  });
});

describe("documentos A4", () => {
  it("DRE e plano de produção escapam o texto", () => {
    const dre = documentoDre({
      ano: "2026", periodo: "11", incluirPrevistos: true, titulo: "Novembro <2026>", anos: ["2026"], linhas: [{ tipo: "final", rotulo: "Resultado líquido", valor: -10, pct: null }],
      resultadoPorEvento: [], unidade: "", regime: "competencia", ratear: true, descricaoRegime: "por competência", rateio: { buffet: 50, restaurante: 30, chocolates: 20 },
      rateioDefinido: true, semUnidade: 0, comparativo: null, categoriasInvestimento: [], resultado: -10,
    });
    expect(dre).toContain("DRE · Novembro &lt;2026&gt;");
    expect(dre).toContain('class="r neg"');
    const plano = documentoPlano({
      plano: { id: 1, titulo: "Doces & cia", unidade: "chocolates", status: "planejado", dataInicio: "2026-11-18", dataFim: "2026-11-18", obs: "", itens: [], previsao: {}, criadoEm: "", atualizadoEm: "" },
      linhas: [], insumos: [], compras: { kpis: { necessarios: 0, aComprar: 0, custo: 0, semPreco: 0, concluidos: 0 }, itens: [] },
    });
    expect(plano).toContain("Doces &amp; cia");
    expect(plano).toContain("Chocolates Marie &amp; Anne · Produção");
    expect(nomeArquivo("DRE · Março 2026")).toBe("dre-marco-2026.html");
  });
});
