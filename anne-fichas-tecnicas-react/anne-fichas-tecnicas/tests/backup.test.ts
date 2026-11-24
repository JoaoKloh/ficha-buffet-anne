import { describe, expect, it } from "vitest";
import { BackupArquivoSchema, RestaurarBackupRequestSchema } from "@/lib/models/backup";

describe("contrato do arquivo de backup", () => {
  it("aceita o arquivo exportado e recusa o que não é backup", () => {
    const arquivo = { app: "Anne Gestão", exportadoEm: "2026-11-10T12:00:00.000Z", colecoes: { eventos: { "7": { nome: "Evento 7" } } } };
    expect(BackupArquivoSchema.parse(arquivo).exportadoEm).toBe("2026-11-10T12:00:00.000Z");
    expect(() => BackupArquivoSchema.parse({ foo: 1 })).toThrow("Este arquivo não é um backup do Anne Gestão.");
    expect(() => RestaurarBackupRequestSchema.parse({ modo: "apagar", arquivo: { colecoes: {} } })).toThrow();
  });
});
