import { describe, expect, it } from "vitest";
import { marcas } from "@/lib/utils/graficos";

describe("gráficos do Painel", () => {
  it("marcas do eixo redondas, cobrindo o intervalo", () => {
    expect(marcas(0, 9300)).toEqual([0, 2500, 5000, 7500, 10000]);
    expect(marcas(-1000, 3000)).toEqual([-1000, 0, 1000, 2000, 3000]);
    expect(marcas(0, 3, 4, true)).toEqual([0, 1, 2, 3]);
    expect(marcas(0, 0)).toEqual([0, 0.25, 0.5, 0.75, 1]);
  });
});
