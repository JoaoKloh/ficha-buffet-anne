import { apiClient } from "../client";
import type {
  AdicionarPratosCardapioRequestDTO,
  AssociarEquipeRequestDTO,
  AssociarEquipeResponseDTO,
  AjustarEtapaRequestDTO,
  AtualizarContratoRequestDTO,
  LancarEquipeResponseDTO,
  PessoaEquipeDTO,
  SalvarLogisticaRequestDTO,
  SalvarOrcamentoRequestDTO,
  SalvarPessoaEquipeRequestDTO,
  CreateEventoRequestDTO,
  CreateExtraRequestDTO,
  CreateItemCardapioRequestDTO,
  EventoDetalheResponseDTO,
  FormOpcoesEvento,
  OpcoesEventoDe,
  EventoResponseDTO,
  EventosResponseDTO,
  FiltroEvento,
  ImportarCardapioAnaliseResponseDTO,
  ImportarCardapioRequestDTO,
  PatchEventoRequestDTO,
  PatchItemCardapioRequestDTO,
  RegistrarContratoRequestDTO,
  UpdateEventoRequestDTO,
  UpdateExtraRequestDTO,
  UpdateItemCardapioRequestDTO,
  UpdateParcelaRequestDTO,
} from "@/lib/models/evento";

/** Eventos, contrato, parcelas, extras e cardápio, via BFF /api/gestao. */
const B = "/api/gestao";
export const listarEventos = (filtro: FiltroEvento, busca: string) => apiClient.get<EventosResponseDTO>(`${B}/eventos?filtro=${filtro}&busca=${encodeURIComponent(busca)}`);
/** Opções de um formulário: cada um pede só os campos que usa. */
export const opcoesEvento = <F extends FormOpcoesEvento>(para: F) => apiClient.get<OpcoesEventoDe[F]>(`${B}/eventos/opcoes?para=${para}`);
export const detalheEvento = (id: number) => apiClient.get<EventoDetalheResponseDTO>(`${B}/eventos/${id}`);
export const criarEvento = (dto: CreateEventoRequestDTO) => apiClient.post<EventoResponseDTO>(`${B}/eventos`, dto);
export const atualizarEvento = (dto: UpdateEventoRequestDTO) => apiClient.put<EventoResponseDTO>(`${B}/eventos`, dto);
export const patchEvento = (id: number, dto: PatchEventoRequestDTO) => apiClient.patch<EventoResponseDTO>(`${B}/eventos/${id}`, dto);
export const excluirEvento = (id: number) => apiClient.delete<void>(`${B}/eventos?id=${id}`);

export const registrarContrato = (id: number, dto: RegistrarContratoRequestDTO) => apiClient.post<{ ok: true }>(`${B}/eventos/${id}/contrato`, dto);
export const atualizarContrato = (id: number, dto: AtualizarContratoRequestDTO) => apiClient.put<{ ok: true }>(`${B}/eventos/${id}/contrato`, dto);
export const excluirContrato = (id: number) => apiClient.delete<void>(`${B}/eventos/${id}/contrato`);

export const atualizarParcela = (dto: UpdateParcelaRequestDTO) => apiClient.put<{ ok: true }>(`${B}/parcelas`, dto);
export const excluirParcela = (id: number) => apiClient.delete<void>(`${B}/parcelas?id=${id}`);

export const criarExtra = (dto: CreateExtraRequestDTO) => apiClient.post(`${B}/extras`, dto);
export const atualizarExtra = (dto: UpdateExtraRequestDTO) => apiClient.put(`${B}/extras`, dto);
export const excluirExtra = (id: number) => apiClient.delete<void>(`${B}/extras?id=${id}`);

export const criarItemCardapio = (dto: CreateItemCardapioRequestDTO) => apiClient.post(`${B}/cardapio`, dto);
export const atualizarItemCardapio = (dto: UpdateItemCardapioRequestDTO) => apiClient.put(`${B}/cardapio`, dto);
export const patchItemCardapio = (id: number, dto: PatchItemCardapioRequestDTO) => apiClient.patch(`${B}/cardapio/${id}`, dto);
export const excluirItemCardapio = (id: number) => apiClient.delete<void>(`${B}/cardapio?id=${id}`);
export const adicionarPratosCardapio = (id: number, dto: AdicionarPratosCardapioRequestDTO) => apiClient.post<{ adicionados: number }>(`${B}/eventos/${id}/cardapio`, dto);
export const copiarCardapio = (id: number, origemEventoId: number) => apiClient.post<{ copiados: number }>(`${B}/eventos/${id}/cardapio/copiar`, { origemEventoId });
export const analisarCardapio = (id: number, texto: string) => apiClient.post<ImportarCardapioAnaliseResponseDTO>(`${B}/eventos/${id}/cardapio/importar`, { texto });
export const confirmarCardapio = (id: number, dto: ImportarCardapioRequestDTO) => apiClient.put<{ adicionados: number }>(`${B}/eventos/${id}/cardapio/importar`, dto);

/* Evento como projeto: escala, logística, orçamento, etapas e fechamento. */
export const criarPessoaEquipe = (id: number, dto: SalvarPessoaEquipeRequestDTO) => apiClient.post<PessoaEquipeDTO>(`${B}/eventos/${id}/equipe`, dto);
export const atualizarPessoaEquipe = (id: number, escalaId: number, dto: SalvarPessoaEquipeRequestDTO) =>
  apiClient.put<PessoaEquipeDTO>(`${B}/eventos/${id}/equipe?pessoa=${escalaId}`, dto);
/** Checklist da escala: manda a escala inteira de uma vez (um pedido para dezenas de pessoas). */
export const associarEquipe = (id: number, dto: AssociarEquipeRequestDTO) => apiClient.post<AssociarEquipeResponseDTO>(`${B}/eventos/${id}/equipe/associar`, dto);
export const excluirPessoaEquipe = (id: number, escalaId: number) => apiClient.delete<void>(`${B}/eventos/${id}/equipe?pessoa=${escalaId}`);
/** Lança em contas a pagar as diárias da escala que ainda estão fora do caixa. */
export const lancarEquipe = (id: number) => apiClient.post<LancarEquipeResponseDTO>(`${B}/eventos/${id}/equipe/lancar`, {});
export const salvarLogistica = (id: number, dto: SalvarLogisticaRequestDTO) => apiClient.put<{ ok: true }>(`${B}/eventos/${id}/logistica`, dto);
export const salvarOrcamento = (id: number, dto: SalvarOrcamentoRequestDTO) => apiClient.put<{ ok: true }>(`${B}/eventos/${id}/orcamento`, dto);
export const ajustarEtapa = (id: number, dto: AjustarEtapaRequestDTO) => apiClient.patch<{ ok: true }>(`${B}/eventos/${id}/etapas`, dto);
/** Passa o evento para Acertado e guarda o retrato do resultado. */
export const fecharEvento = (id: number) => apiClient.post<EventoResponseDTO>(`${B}/eventos/${id}/fechar`, {});
