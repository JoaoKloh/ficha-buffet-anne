import { java } from "./java";
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

/** Cozinha: CozinhaController (/cozinha) do backend Java. */
export const cozinhaBackend = {
  tela: () => java.get<CozinhaResponseDTO>("/cozinha/retornarTudo"),
  salvar: (dto: SalvarExecucaoRequestDTO) => java.post<SalvarExecucaoResponseDTO>("/cozinha/execucao/salvar", dto),
  avaria: async (dto: AvariaRequestDTO) => (await java.post<{ lancado: number }>("/cozinha/avaria/lancar", dto)).lancado,

  // Ordens de produção, mapa, setores e liberação (CozinhaController).
  agirOrdem: ({ acao, ...dto }: AcaoOrdemRequestDTO) => java.post<void>(`/cozinha/ordens/${acao}`, dto),
  planejarOrdem: ({ eventoId, ...dto }: PlanoOrdemRequestDTO) => java.put<void>(`/cozinha/mapa/plano/${eventoId}`, dto),
  gerarMapa: (eventoId: number) => java.post<void>(`/cozinha/mapa/gerar/${eventoId}`),
  salvarMestre: ({ eventoId, ...dto }: MestreMapaRequestDTO) => java.put<void>(`/cozinha/mapa/mestre/${eventoId}`, dto),
  liberarMapa: ({ eventoId, ...dto }: LiberarMapaRequestDTO) => java.put<void>(`/cozinha/mapa/liberar/${eventoId}`, dto),
  salvarSetores: (dto: SalvarSetoresRequestDTO) => java.put<void>("/cozinha/setores/salvar", dto),

  salvarLiberacao: ({ eventoId, ...dto }: SalvarLiberacaoRequestDTO) => java.put<void>(`/cozinha/liberacao/salvar/${eventoId}`, dto),
  autorizarLiberacao: ({ eventoId, ...dto }: AutorizarLiberacaoRequestDTO) => java.post<void>(`/cozinha/liberacao/autorizar/${eventoId}`, dto),
  desfazerLiberacao: (eventoId: number) => java.post<void>(`/cozinha/liberacao/desfazer/${eventoId}`),

  // Ocorrências: endpoints Java ainda a criar no CozinhaController.
  criarOcorrencia: (dto: CriarOcorrenciaRequestDTO) => java.post<OcorrenciaDTO>("/cozinha/ocorrencias/criar", dto),
  resolverOcorrencia: (id: number, resolvida: boolean) => java.patch<OcorrenciaDTO>(`/cozinha/ocorrencias/resolver/${id}`, { resolvida }),
};
