import { z } from "zod";

/* ----------------------------- Backup e restauração ----------------------------- */

/**
 * Arquivo de backup (.json): tudo que está cadastrado, por coleção e id.
 * Mesmo formato do protótipo de referência.
 */
export const APP_BACKUP = "Anne Gestão";
export const BackupArquivoSchema = z.object(
  {
    app: z.string().optional(),
    exportadoEm: z.string().optional(),
    colecoes: z.record(z.record(z.unknown()), { message: "Este arquivo não é um backup do Anne Gestão." }),
  },
  { message: "Este arquivo não é um backup do Anne Gestão." }
);
export type BackupArquivoDTO = z.infer<typeof BackupArquivoSchema>;

export interface UltimoBackupDTO {
  ultimoEm: string; // ISO data-hora
  tipo: "json";
  contagens: Record<string, number>;
  total: number;
}

export interface BackupResumoResponseDTO {
  ultimo: UltimoBackupDTO | null;
  contagens: { colecao: string; rotulo: string; n: number }[];
  total: number;
}

export const MODOS_RESTAURACAO = ["faltantes", "todos"] as const;
export type ModoRestauracao = (typeof MODOS_RESTAURACAO)[number];

export const AnalisarBackupRequestSchema = z.object({ arquivo: BackupArquivoSchema });
export type AnalisarBackupRequestDTO = z.infer<typeof AnalisarBackupRequestSchema>;

export interface AnaliseBackupLinhaDTO {
  colecao: string;
  rotulo: string;
  total: number;
  novos: number;
  diferentes: number;
  iguais: number;
  soNoSistema: number;
}
export interface AnaliseBackupResponseDTO {
  exportadoEm: string;
  linhas: AnaliseBackupLinhaDTO[];
  ignoradas: string[];
  invalidos: number;
}

export const RestaurarBackupRequestSchema = z.object({
  nomeArquivo: z.string().trim().max(120).default(""),
  modo: z.enum(MODOS_RESTAURACAO),
  arquivo: BackupArquivoSchema,
});
export type RestaurarBackupRequestDTO = z.infer<typeof RestaurarBackupRequestSchema>;

export interface RestaurarBackupResponseDTO {
  total: number;
  gravados: number;
  falhas: { colecao: string; rotulo: string; id: string }[];
  /** Registros relidos do sistema e iguais ao backup. */
  conferidos: number;
}
