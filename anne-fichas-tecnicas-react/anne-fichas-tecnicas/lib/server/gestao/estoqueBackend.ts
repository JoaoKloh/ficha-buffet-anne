import { java, query } from "./java";
import type {
  EstoqueComprasResponseDTO,
  EstoqueConferenciaResponseDTO,
  EstoquePerdasResponseDTO,
  EstoqueValidadesResponseDTO,
  MarcarPedidoRequestDTO,
  CreateLoteRequestDTO,
  CreateMovimentoRequestDTO,
  EstoqueLoucasResponseDTO,
  EstoqueOpcoesResponseDTO,
  FormOpcoesEstoque,
  EstoquePreparosResponseDTO,
  EstoqueSaldoResponseDTO,
  InventarioRequestDTO,
  ItemEstoqueDetalheDTO,
  LoteListaItemDTO,
  LoteResponseDTO,
  MovimentarLoteRequestDTO,
  MovimentoResponseDTO,
  MovimentosResponseDTO,
  NfDeParaRequestDTO,
  NotaFiscalImportadaDTO,
  NotaFiscalListaItemDTO,
  NotaFiscalResponseDTO,
  SalvarNotaFiscalRequestDTO,
  UpdateLoteRequestDTO,
} from "@/lib/models/estoque";

/**
 * Estoque, lotes de pré-preparo e notas fiscais no backend Java:
 * EstoqueController, MovimentoEstoqueController, LotePreparoController e NotaFiscalController.
 */
export const estoqueBackend = {
  opcoes: (para: FormOpcoesEstoque) => java.get<Partial<EstoqueOpcoesResponseDTO>>(`/estoque/opcoes?para=${para}`),
  saldo: () => java.get<EstoqueSaldoResponseDTO>("/estoque/saldo"),
  item: (id: number) => java.get<ItemEstoqueDetalheDTO>(`/estoque/item/${id}`),
  loucas: (data: string | null) => java.get<EstoqueLoucasResponseDTO>(`/estoque/loucas${query({ data })}`),
  // Estoque integrado: compras sugeridas, pedido feito, validades, perdas e conferência.
  compras: (horizonte: number) => java.get<EstoqueComprasResponseDTO>(`/estoque/compras${query({ horizonte })}`),
  marcarPedido: (dto: MarcarPedidoRequestDTO) => java.put<void>("/estoque/compras/marcarPedido", dto),
  validades: () => java.get<EstoqueValidadesResponseDTO>("/estoque/validades"),
  perdas: (mes: string) => java.get<EstoquePerdasResponseDTO>(`/estoque/perdas${query({ mes })}`),
  conferencia: () => java.get<EstoqueConferenciaResponseDTO>("/estoque/conferencia"),

  movimentos: (filtro: string) => java.get<MovimentosResponseDTO>(`/movimentos/retornarTodos${query({ filtro })}`),
  criarMovimento: (dto: CreateMovimentoRequestDTO) => java.post<MovimentoResponseDTO>("/movimentos/criar", dto),
  apagarMovimento: (id: number) => java.delete(`/movimentos/apagar/${id}`),
  inventario: async (dto: InventarioRequestDTO) => (await java.post<{ lancados: number }>("/movimentos/inventario", dto)).lancados,

  preparos: (filtro: string) => java.get<EstoquePreparosResponseDTO>(`/lotes/retornarTodos${query({ filtro })}`),
  lote: (id: number) => java.get<LoteListaItemDTO>(`/lotes/detalhar/${id}`),
  criarLote: (dto: CreateLoteRequestDTO) => java.post<LoteResponseDTO>("/lotes/criar", dto),
  /** Lote sem uso: corpo completo. Com uso: só validade, responsável e observações. */
  atualizarLote: (id: number, dto: CreateLoteRequestDTO | Omit<UpdateLoteRequestDTO, "id">) => java.put<LoteResponseDTO>(`/lotes/atualizar/${id}`, dto),
  movimentarLote: (id: number, dto: MovimentarLoteRequestDTO) => java.post<LoteResponseDTO>(`/lotes/movimentar/${id}`, dto),
  apagarLote: (id: number) => java.delete(`/lotes/apagar/${id}`),

  listarNfs: () => java.get<NotaFiscalListaItemDTO[]>("/notasfiscais/retornarTodos"),
  obterNf: (id: number) => java.get<NotaFiscalListaItemDTO>(`/notasfiscais/detalhar/${id}`),
  salvarRascunho: (id: number | null, dto: SalvarNotaFiscalRequestDTO) =>
    id == null ? java.post<NotaFiscalResponseDTO>("/notasfiscais/salvarRascunho", dto) : java.put<NotaFiscalResponseDTO>(`/notasfiscais/salvarRascunho/${id}`, dto),
  /** Dar entrada (rascunho existente ou nota nova): estoque, preço dos itens e contas a pagar. */
  lancarNf: (id: number | null, dto: SalvarNotaFiscalRequestDTO) => java.post<NotaFiscalResponseDTO>(id == null ? "/notasfiscais/lancar" : `/notasfiscais/lancar/${id}`, dto),
  estornarNf: (id: number) => java.post<void>(`/notasfiscais/estornar/${id}`),
  apagarNf: (id: number) => java.delete(`/notasfiscais/apagar/${id}`),
  importarXml: (xml: string) => java.post<NotaFiscalImportadaDTO>("/notasfiscais/importarXml", { xml }),
  dePara: (dto: NfDeParaRequestDTO) => java.post<({ insumoId: number; unid: "estoque" | "compra" } | null)[]>("/notasfiscais/sugerirItens", dto),
};
