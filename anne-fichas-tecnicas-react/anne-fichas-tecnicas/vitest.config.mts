import { defineConfig } from "vitest/config";
import path from "path";

// Testes de unidade dos cálculos e contratos (sem DOM, sem servidor).
export default defineConfig({
  resolve: { alias: { "@": path.resolve(__dirname, ".") } },
  test: { include: ["tests/**/*.test.ts"], environment: "node" },
});
