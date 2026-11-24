import { java } from "./java";
import type {
  AnaliseBackupResponseDTO,
  BackupArquivoDTO,
  BackupResumoResponseDTO,
  RestaurarBackupRequestDTO,
  RestaurarBackupResponseDTO,
  UltimoBackupDTO,
} from "@/lib/models/backup";

/** Backup e restauração: BackupController (/backup) do backend Java. */
export const backupBackend = {
  resumo: () => java.get<BackupResumoResponseDTO>("/backup/resumo"),
  exportar: () => java.get<BackupArquivoDTO>("/backup/exportar"),
  registrar: () => java.post<UltimoBackupDTO>("/backup/registrar"),
  planilha: () => java.bytes("/backup/planilha"),
  analisar: (arquivo: BackupArquivoDTO) => java.post<AnaliseBackupResponseDTO>("/backup/analisar", { arquivo }),
  restaurar: (dto: RestaurarBackupRequestDTO) => java.post<RestaurarBackupResponseDTO>("/backup/restaurar", dto),
};
