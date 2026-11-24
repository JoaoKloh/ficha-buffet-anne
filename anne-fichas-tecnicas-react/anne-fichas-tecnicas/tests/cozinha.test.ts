import { describe, expect, it } from "vitest";
import { czArred, czEnvV, czExtraQ, czGate, czLoucaSit, czRowSt, czRows, tempFora } from "@/lib/utils/checklist";
import { SalvarExecucaoRequestSchema } from "@/lib/models/cozinha";
import { pratoProjecao, projecao } from "./fixtures";

/** Projeção como o backend devolve: coxinha sem componentes (viajam os insumos da ficha), ceviche com componente e uma louça. */
const pj = projecao({
  pratos: [
    pratoProjecao({ cid: 100, nome: "Coxinha", cat: "volante", total: 200, ins: [{ k: "1_", n: "Frango", u: "kg", via: "", cons: "refrigerado", qb: 2, qt: 40 }] }),
    pratoProjecao({ cid: 101, nome: "Ceviche", cat: "ilha", uu: "kg", ulabel: "kg", total: 5, comp: [{ k: "c0", n: "Leite de tigre", u: "L", q: 0.02, prev: 0.1, cons: "refrigerado", obs: "" }] }),
    pratoProjecao({ cid: 102, nome: "Sobremesa a definir", cat: "sobremesa", st: "definir" }),
  ],
  loucas: [{ id: 2, n: "Bandeja", q: 20, usos: "Coxinha" }],
});

describe("checklist de envio", () => {
  const rows = czRows(pj);
  const prato = rows.find((r) => r.id === "i_100")!;
  const comp = rows.find((r) => r.id === "c_101__c0")!;
  const louca = rows.find((r) => r.tipo === "louca")!;

  it("linhas: prato, componentes (ou insumos da ficha) e louças; 'a definir' fica de fora", () => {
    expect(rows.map((r) => r.id)).toEqual(["i_101", "c_101__c0", "i_100", "c_100__f_1_", "l_1__2"]);
    expect(comp).toMatchObject({ nome: "Leite de tigre", prev: 0.1, cons: "refrigerado" });
    expect(louca).toMatchObject({ prev: 20, envK: "saiu", retK: "voltou" });
  });

  it("só sai com produção, duas assinaturas e temperatura na faixa", () => {
    const ok = { produzido: 200, executadoPor: "Ana", separadoPor: "Bia" };
    expect(czGate(prato, ok)).toBe(false); // refrigerado sem temperatura
    expect(czGate(prato, { ...ok, tempSaida: 3 })).toBe(true);
    expect(czGate(prato, { ...ok, tempSaida: 9 })).toBe(false);
    expect(czGate(prato, { ...ok, executadoPor: "", tempSaida: 3 })).toBe(false);
    expect(czGate(louca, { separadoPor: "Bia" })).toBe(true);
  });

  it("situação da linha", () => {
    expect(czRowSt(prato, {})).toEqual(["", "Pendente"]);
    expect(czRowSt(prato, { produzido: 200 })).toEqual(["warn", "Faltam assinaturas ou temp."]);
    expect(czRowSt(prato, { produzido: 200, executadoPor: "A", separadoPor: "B", tempSaida: 2 })).toEqual(["accent", "Pronto para sair"]);
    expect(czRowSt(prato, { produzido: 200, enviadoQ: 200, tempSaida: 2 })).toEqual(["good", "Enviado"]);
    expect(czRowSt(prato, { tempSaida: 8 })).toEqual(["bad", "Temperatura fora"]);
    expect(czEnvV(prato, { enviado: true, produzido: 180 })).toBe(180);
  });

  it("louça: fora, devolvida, faltando e avaria", () => {
    expect(czLoucaSit({})).toEqual(["", "No depósito"]);
    expect(czLoucaSit({ saiu: 20 })).toEqual(["brass", "Fora no evento"]);
    expect(czLoucaSit({ saiu: 20, voltou: 18 })).toEqual(["bad", "Faltam 2"]);
    expect(czLoucaSit({ saiu: 20, voltou: 18, avaria: 2 })).toEqual(["good", "Devolvida (avaria lançada)"]);
  });

  it("faixas de temperatura e arredondamentos", () => {
    expect([tempFora("refrigerado", 4), tempFora("refrigerado", 4.5), tempFora("congelado", -18), tempFora("congelado", -10), tempFora("quente", 59), tempFora("ambiente", 30), tempFora("quente", null)]).toEqual([false, true, false, true, true, false, null]);
    expect([czArred(1.23, "kg"), czArred(0.01, "L"), czArred(2.1, "un"), czArred(0, "un")]).toEqual([1.2, 0.1, 3, 0]);
    const nd = { base: 100, N: 105 };
    expect(czExtraQ({ modo: "fix", v: 20, u: "un" }, nd)).toBe(20);
    expect(czExtraQ({ modo: "pax", v: 0.35, u: "L" }, nd)).toBe(36.8);
    expect(czExtraQ({ modo: "pax106", v: 3, u: "un" }, nd)).toBe(318);
  });

  it("contrato da gravação: chave, tipo e campos", () => {
    expect(SalvarExecucaoRequestSchema.parse({ chave: "i_100", kind: "item", eventoId: 1, cardapioId: 100, campos: { produzido: 3.5, executadoPor: "Ana" } }).campos.produzido).toBe(3.5);
    expect(() => SalvarExecucaoRequestSchema.parse({ chave: "", kind: "item", eventoId: 1, campos: {} })).toThrow();
    expect(() => SalvarExecucaoRequestSchema.parse({ chave: "i_1", kind: "outro", eventoId: 1, campos: {} })).toThrow();
  });
});
