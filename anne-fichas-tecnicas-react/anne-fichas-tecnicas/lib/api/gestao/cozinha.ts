import { apiClient } from "../client";
import type {
  AcaoOrdemRequestDTO,
  AutorizarLiberacaoRequestDTO,
  AvariaRequestDTO,
  CozinhaResponseDTO,
  CriarOcorrenciaRequestDTO,
  LiberarMapaRequestDTO,
  MestreMapaRequestDTO,
  OcorrenciaDTO,
  PlanoOrdemRequestDTO,
  SalvarExecucaoRequestDTO,
  SalvarExecucaoResponseDTO,
  SalvarLiberacaoRequestDTO,
  SalvarSetoresRequestDTO,
} from "@/lib/models/cozinha";

/** Cozinha (projeções, execução, ordens de produção, liberação e ocorrências), via BFF /api/gestao/cozinha. */
const B = "/api/gestao/cozinha";
export const telaCozinha = () => apiClient.get<CozinhaResponseDTO>(B);
export const salvarExecucao = (dto: SalvarExecucaoRequestDTO) => apiClient.post<SalvarExecucaoResponseDTO>(`${B}/execucao`, dto);
export const lancarAvaria = (dto: AvariaRequestDTO) => apiClient.post<{ lancado: number }>(`${B}/avaria`, dto);

/* Ordens de produção: ações da equipe e plano da gestão. */
export const agirOrdem = (dto: AcaoOrdemRequestDTO) => apiClient.post<{ ok: true }>(`${B}/ordens`, dto);
export const planejarOrdem = (dto: PlanoOrdemRequestDTO) => apiClient.put<{ ok: true }>(`${B}/ordens`, dto);
export const gerarMapa = (eventoId: number) => apiClient.post<{ ok: true }>(`${B}/mapa`, { eventoId });
export const salvarMestre = (dto: MestreMapaRequestDTO) => apiClient.put<{ ok: true }>(`${B}/mapa`, dto);
export const liberarMapa = (dto: LiberarMapaRequestDTO) => apiClient.put<{ ok: true }>(`${B}/mapa/liberar`, dto);
export const salvarSetores = (dto: SalvarSetoresRequestDTO) => apiClient.put<{ ok: true }>(`${B}/setores`, dto);

/* Liberação para expedição. */
export const salvarLiberacao = (dto: SalvarLiberacaoRequestDTO) => apiClient.put<{ ok: true }>(`${B}/liberacao`, dto);
export const autorizarLiberacao = (dto: AutorizarLiberacaoRequestDTO) => apiClient.post<{ ok: true }>(`${B}/liberacao/autorizar`, dto);
export const desfazerLiberacao = (eventoId: number) => apiClient.post<{ ok: true }>(`${B}/liberacao/desfazer`, { eventoId });

/* Ocorrências do evento. */
export const criarOcorrencia = (dto: CriarOcorrenciaRequestDTO) => apiClient.post<OcorrenciaDTO>(`${B}/ocorrencias`, dto);
export const resolverOcorrencia = (id: number, resolvida: boolean) => apiClient.patch<OcorrenciaDTO>(`${B}/ocorrencias?id=${id}`, { resolvida });
