import { describe, expect, it } from "vitest";
import { documentoCozinha, nomeArquivoCozinha, type AbaCozinha } from "@/components/gestao/cozinha/imprimir";
import type { CozinhaResponseDTO } from "@/lib/models/cozinha";
import { exec, pratoProjecao, projecao } from "./fixtures";

const pj = projecao({
  nome: "Casamento & Cia", local: "Serra",
  pratos: [pratoProjecao({ cid: 100, nome: "Ceviche", cat: "ilha", praca: "Frios", modo: "Curar no limão.", ins: [{ k: "1_", n: "Peixe <fresco>", u: "kg", via: "", cons: "refrigerado", qb: 2, qt: 20 }] })],
  compras: [{ id: 1, n: "Peixe <fresco>", u: "kg", cat: "Proteínas e frutos do mar", q: 20, qd: 0, est: 0, fc: 0, uc: "", usos: "Ceviche" }],
});
const tela: CozinhaResponseDTO = {
  projecoes: [pj],
  execucoes: [exec({ id: 1, chave: "i_100", cardapioId: 100, produzido: 100, executadoPor: "Ana" }), exec({ id: 2, chave: "tr_1", kind: "transporte", chefPor: "Chef Lu" })],
  tarefas: [],
};
const doc = (aba: AbaCozinha) => documentoCozinha(tela, { aba, pj, praca: "", semana: "2026-12-07", filtroCompra: "comprar" });

describe("documentos A4 da cozinha", () => {
  it("um documento por aba, com o título certo e texto escapado", () => {
    const titulos: Record<AbaCozinha, string> = {
      agenda: "Agenda da cozinha", producao: "Lista de produção", insumos: "Insumos por prato", compras: "Lista de compras", cronograma: "Cronograma de produção", checklist: "Checklist de produto pronto — saída e retorno",
      lider: "Produções de hoje e atrasadas", indicadores: "Produções de hoje e atrasadas", ordens: "Ordens de produção", ocorrencias: "Cronograma de produção",
    };
    for (const [aba, titulo] of Object.entries(titulos) as [AbaCozinha, string][]) {
      const h = doc(aba);
      expect(h).toContain(`<h1>${titulo}</h1>`);
      expect(h).not.toContain("Casamento & Cia");
      expect(h).not.toContain("<fresco>");
    }
    expect(doc("checklist")).toContain("size:A4 landscape");
  });

  it("produção marca o que já foi produzido; insumos e compras trazem as quantidades do evento", () => {
    expect(doc("producao")).toContain('<td class="c"><span class="bx on">✓</span></td><td><b>Ceviche</b>');
    expect(doc("insumos")).toContain("Peixe &lt;fresco&gt;");
    expect(doc("insumos")).toContain("20 kg");
    expect(doc("compras")).toContain("<h2>Proteínas e frutos do mar<small>1 item</small></h2>");
    expect(doc("checklist")).toContain("<b>Chef Lu</b>Chef responsável pela produção");
    expect(doc("agenda")).toContain("Casamento &amp; Cia (100 pessoas)");
  });

  it("nome do arquivo sem caracteres estranhos", () => {
    expect(nomeArquivoCozinha({ aba: "compras", pj, praca: "", semana: "", filtroCompra: "todos" })).toMatch(/^cozinha-compras-\d{2}-\d{3}-\d{4}-\d{2}-\d{2}\.html$/);
  });
});
