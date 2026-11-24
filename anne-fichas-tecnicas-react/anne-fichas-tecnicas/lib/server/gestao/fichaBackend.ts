import { java, query } from "./java";
import type {
  CalcularCustoRequestDTO,
  CreatePreparacaoRequestDTO,
  CustoPreparacaoDTO,
  FichasOpcoesResponseDTO,
  PreparacaoListaItemDTO,
  PreparacaoResponseDTO,
  UpdatePreparacaoRequestDTO,
} from "@/lib/models/preparacao";
import type { CreateInsumoRequestDTO, InsumoListaItemDTO, InsumoResponseDTO, UpdateInsumoRequestDTO } from "@/lib/models/insumo";
import type { CalcularCmvRequestDTO, CmvDTO, CreatePratoVendaRequestDTO, PratoVendaResponseDTO, PratosVendaResponseDTO, UpdatePratoVendaRequestDTO } from "@/lib/models/pratoVenda";

/**
 * Fichas técnicas, cadastro de itens e pratos da carta no backend Java:
 * PratoController (/prato/fichas), IngredientesController (/ingredientes/itens) e PratoVendaController.
 */
export const fichaBackend = {
  opcoes: () => java.get<FichasOpcoesResponseDTO>("/prato/fichas/opcoes"),

  listarPreparacoes: (busca = "") => java.get<PreparacaoListaItemDTO[]>(`/prato/fichas/retornarTodos${query({ busca })}`),
  calcularCusto: (dto: CalcularCustoRequestDTO) => java.post<CustoPreparacaoDTO>("/prato/fichas/calcularCusto", dto),
  salvarPreparacao(dto: CreatePreparacaoRequestDTO | UpdatePreparacaoRequestDTO): Promise<PreparacaoResponseDTO> {
    if ("id" in dto) {
      const { id, ...corpo } = dto;
      return java.put<PreparacaoResponseDTO>(`/prato/fichas/atualizar/${id}`, corpo);
    }
    return java.post<PreparacaoResponseDTO>("/prato/fichas/criar", dto);
  },
  apagarPreparacao: (id: number) => java.delete(`/prato/fichas/apagar/${id}`),

  listarInsumos: (busca = "") => java.get<InsumoListaItemDTO[]>(`/ingredientes/itens/retornarTodos${query({ busca })}`),
  situacaoInsumo: (id: number) => java.get<{ emFichas: number; temMovimentos: boolean }>(`/ingredientes/itens/situacao/${id}`),
  salvarInsumo(dto: CreateInsumoRequestDTO | UpdateInsumoRequestDTO): Promise<InsumoResponseDTO> {
    if ("id" in dto) {
      const { id, ...corpo } = dto;
      return java.put<InsumoResponseDTO>(`/ingredientes/itens/atualizar/${id}`, corpo);
    }
    return java.post<InsumoResponseDTO>("/ingredientes/itens/criar", dto);
  },
  apagarInsumo: (id: number) => java.delete(`/ingredientes/itens/apagar/${id}`),

  telaPratosVenda: (busca = "") => java.get<PratosVendaResponseDTO>(`/pratosvenda/retornarTodos${query({ busca })}`),
  calcularCmv: (dto: CalcularCmvRequestDTO) => java.post<CmvDTO>("/pratosvenda/calcularCusto", dto),
  salvarPratoVenda(dto: CreatePratoVendaRequestDTO | UpdatePratoVendaRequestDTO): Promise<PratoVendaResponseDTO> {
    if ("id" in dto) {
      const { id, ...corpo } = dto;
      return java.put<PratoVendaResponseDTO>(`/pratosvenda/atualizar/${id}`, corpo);
    }
    return java.post<PratoVendaResponseDTO>("/pratosvenda/criar", dto);
  },
  apagarPratoVenda: (id: number) => java.delete(`/pratosvenda/apagar/${id}`),
};
