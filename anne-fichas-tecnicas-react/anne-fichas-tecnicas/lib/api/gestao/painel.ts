import { apiClient } from "../client";
import type { PainelResumoResponseDTO } from "@/lib/models/painel";
import type { AnaliseBackupResponseDTO, BackupArquivoDTO, BackupResumoResponseDTO, ModoRestauracao, RestaurarBackupResponseDTO, UltimoBackupDTO } from "@/lib/models/backup";

/** Painel e backup, via BFF /api/gestao. */
const B = "/api/gestao";
export const resumoPainel = () => apiClient.get<PainelResumoResponseDTO>(`${B}/painel`);

export const resumoBackup = () => apiClient.get<BackupResumoResponseDTO>(`${B}/backup`);
export const exportarBackup = () => apiClient.get<BackupArquivoDTO>(`${B}/backup/exportar`);
export const registrarBackup = () => apiClient.post<UltimoBackupDTO>(`${B}/backup`, {});
/** A planilha é um download direto do navegador (arquivo binário, sem envelope). */
export const URL_PLANILHA_BACKUP = `${B}/backup/planilha`;
export const analisarBackup = (arquivo: BackupArquivoDTO) => apiClient.post<AnaliseBackupResponseDTO>(`${B}/backup/analisar`, { arquivo });
export const restaurarBackup = (nomeArquivo: string, arquivo: BackupArquivoDTO, modo: ModoRestauracao) =>
  apiClient.post<RestaurarBackupResponseDTO>(`${B}/backup/restaurar`, { nomeArquivo, arquivo, modo });
