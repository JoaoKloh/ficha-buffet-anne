import { java, query } from "./java";
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
  EventoOpcoesResponseDTO,
  FormOpcoesEvento,
  EventoResponseDTO,
  EventosResponseDTO,
  ExtraResponseDTO,
  FiltroEvento,
  ImportarCardapioAnaliseResponseDTO,
  ImportarCardapioRequestDTO,
  ItemCardapioResponseDTO,
  PatchEventoRequestDTO,
  PatchItemCardapioRequestDTO,
  RegistrarContratoRequestDTO,
  UpdateEventoRequestDTO,
  UpdateExtraRequestDTO,
  UpdateItemCardapioRequestDTO,
  UpdateParcelaRequestDTO,
} from "@/lib/models/evento";

/**
 * Eventos, contrato, parcelas, extras e cardápio no backend Java:
 * ProducaoController (/producao/eventos), ParcelaController, ExtraEventoController e ItemCardapioController.
 */
const E = "/producao/eventos";

export const eventoBackend = {
  listar: (filtro: FiltroEvento, busca: string) => java.get<EventosResponseDTO>(`${E}/retornarTodos${query({ filtro, busca })}`),
  opcoes: (para: FormOpcoesEvento) => java.get<Partial<EventoOpcoesResponseDTO>>(`${E}/opcoes?para=${para}`),
  detalhe: (id: number) => java.get<EventoDetalheResponseDTO>(`${E}/detalhar/${id}`),
  criar: (dto: CreateEventoRequestDTO) => java.post<EventoResponseDTO>(`${E}/criar`, dto),
  atualizar: ({ id, ...dto }: UpdateEventoRequestDTO) => java.put<EventoResponseDTO>(`${E}/atualizar/${id}`, dto),
  patch: (id: number, dto: PatchEventoRequestDTO) => java.patch<EventoResponseDTO>(`${E}/ajustar/${id}`, dto),
  apagar: (id: number) => java.delete(`${E}/apagar/${id}`),

  registrarContrato: (id: number, dto: RegistrarContratoRequestDTO) => java.post<void>(`${E}/contrato/registrar/${id}`, dto),
  atualizarContrato: (id: number, dto: AtualizarContratoRequestDTO) => java.put<void>(`${E}/contrato/atualizar/${id}`, dto),
  apagarContrato: (id: number) => java.delete(`${E}/contrato/apagar/${id}`),

  atualizarParcela: ({ id, ...dto }: UpdateParcelaRequestDTO) => java.put<void>(`/parcelas/atualizar/${id}`, dto),
  apagarParcela: (id: number) => java.delete(`/parcelas/apagar/${id}`),

  salvarExtra(dto: CreateExtraRequestDTO | UpdateExtraRequestDTO): Promise<ExtraResponseDTO> {
    if ("id" in dto) {
      const { id, ...corpo } = dto;
      return java.put<ExtraResponseDTO>(`/extras/atualizar/${id}`, corpo);
    }
    return java.post<ExtraResponseDTO>("/extras/criar", dto);
  },
  apagarExtra: (id: number) => java.delete(`/extras/apagar/${id}`),

  salvarItemCardapio(dto: CreateItemCardapioRequestDTO | UpdateItemCardapioRequestDTO): Promise<ItemCardapioResponseDTO> {
    if ("id" in dto) {
      const { id, ...corpo } = dto;
      return java.put<ItemCardapioResponseDTO>(`/cardapio/atualizar/${id}`, corpo);
    }
    return java.post<ItemCardapioResponseDTO>("/cardapio/criar", dto);
  },
  patchItemCardapio: (id: number, dto: PatchItemCardapioRequestDTO) => java.patch<ItemCardapioResponseDTO>(`/cardapio/ajustar/${id}`, dto),
  apagarItemCardapio: (id: number) => java.delete(`/cardapio/apagar/${id}`),
  /** Seleção múltipla: o Java ignora as fichas que já estão no cardápio e devolve quantas entraram. */
  adicionarPratos: async (id: number, dto: AdicionarPratosCardapioRequestDTO) => (await java.post<{ adicionados: number }>(`/cardapio/adicionarVarios/${id}`, dto)).adicionados,
  copiarCardapio: async (id: number, origemEventoId: number) => (await java.post<{ copiados: number }>(`/cardapio/copiar/${id}`, { origemEventoId })).copiados,
  analisarImportacao: (id: number, texto: string) => java.post<ImportarCardapioAnaliseResponseDTO>(`/cardapio/analisarImportacao/${id}`, { texto }),
  confirmarImportacao: async (id: number, dto: ImportarCardapioRequestDTO) => (await java.post<{ adicionados: number }>(`/cardapio/confirmarImportacao/${id}`, dto)).adicionados,

  // Evento como projeto (endpoints Java ainda a criar no ProducaoController, sob /producao/eventos).
  criarPessoaEquipe: (id: number, dto: SalvarPessoaEquipeRequestDTO) => java.post<PessoaEquipeDTO>(`${E}/equipe/criar/${id}`, dto),
  atualizarPessoaEquipe: (id: number, escalaId: number, dto: SalvarPessoaEquipeRequestDTO) =>
    java.put<PessoaEquipeDTO>(`${E}/equipe/atualizar/${id}/${escalaId}`, dto),
  apagarPessoaEquipe: (id: number, escalaId: number) => java.delete(`${E}/equipe/apagar/${id}/${escalaId}`),
  associarEquipe: (id: number, dto: AssociarEquipeRequestDTO) => java.post<AssociarEquipeResponseDTO>(`${E}/equipe/associar/${id}`, dto),
  lancarEquipe: (id: number) => java.post<LancarEquipeResponseDTO>(`${E}/equipe/lancar/${id}`),
  salvarLogistica: (id: number, dto: SalvarLogisticaRequestDTO) => java.put<void>(`${E}/logistica/salvar/${id}`, dto),
  salvarOrcamento: (id: number, dto: SalvarOrcamentoRequestDTO) => java.put<void>(`${E}/orcamento/salvar/${id}`, dto),
  ajustarEtapa: (id: number, dto: AjustarEtapaRequestDTO) => java.patch<void>(`${E}/etapas/ajustar/${id}`, dto),
  fechar: (id: number) => java.post<EventoResponseDTO>(`${E}/fechar/${id}`),
};
