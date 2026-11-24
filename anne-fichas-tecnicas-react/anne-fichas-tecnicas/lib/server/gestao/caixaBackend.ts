import { java, query } from "./java";
import type {
  CaixaMesResponseDTO,
  CategoriaResponseDTO,
  ClassificarRequestDTO,
  ContasResponseDTO,
  CreateCategoriaRequestDTO,
  CreateLancamentoRequestDTO,
  DreResponseDTO,
  FiltroCaixa,
  FiltroContas,
  FiltroUnidade,
  FluxoCaixaResponseDTO,
  LancamentoListaItemDTO,
  LancamentoResponseDTO,
  PagamentoRequestDTO,
  RateioRequestDTO,
  RegimeDre,
  SaldosRequestDTO,
  SemUnidadeResponseDTO,
  TransferenciaRequestDTO,
  TransferenciasResponseDTO,
  UnidadeDre,
  UpdateCategoriaRequestDTO,
  UpdateLancamentoRequestDTO,
  UpdateTransferenciaRequestDTO,
} from "@/lib/models/caixa";

/** Caixa e DRE: LancamentoController (/lancamentos) e CategoriaFinanceiraController do backend Java. */
export const caixaBackend = {
  tela: (mes: string, filtro: FiltroCaixa, unidade: FiltroUnidade) =>
    java.get<CaixaMesResponseDTO>(`/lancamentos/caixa/retornarMes${query({ mes, filtro, unidade: unidade || undefined })}`),
  contas: (tipo: "pagar" | "receber", filtro: FiltroContas, unidade: FiltroUnidade) =>
    java.get<ContasResponseDTO>(`/lancamentos/caixa/contas${query({ tipo, filtro, unidade: unidade || undefined })}`),
  fluxo: (unidade: FiltroUnidade) => java.get<FluxoCaixaResponseDTO>(`/lancamentos/caixa/fluxo${query({ unidade: unidade || undefined })}`),
  transferencias: (unidade: FiltroUnidade) =>
    java.get<TransferenciasResponseDTO>(`/lancamentos/caixa/transferencias${query({ unidade: unidade || undefined })}`),
  semUnidade: () => java.get<SemUnidadeResponseDTO>("/lancamentos/caixa/semUnidade"),
  dre: (ano: string, periodo: string, incluirPrevistos: boolean, unidade: UnidadeDre, regime: RegimeDre, ratear: boolean) =>
    java.get<DreResponseDTO>(`/lancamentos/dre${query({ ano, periodo, previstos: incluirPrevistos, unidade: unidade || undefined, regime, ratear })}`),

  obterLancamento: (id: number) => java.get<LancamentoListaItemDTO>(`/lancamentos/caixa/detalhar/${id}`),
  criarLancamento: (dto: CreateLancamentoRequestDTO) => java.post<LancamentoResponseDTO>("/lancamentos/caixa/criar", dto),
  atualizarLancamento: ({ id, ...dto }: UpdateLancamentoRequestDTO) => java.put<LancamentoResponseDTO>(`/lancamentos/caixa/atualizar/${id}`, dto),
  apagarLancamento: (id: number) => java.delete(`/lancamentos/apagar/${id}`),
  marcarPago: (id: number) => java.post<void>(`/lancamentos/caixa/pagar/${id}`),
  registrarPagamento: (id: number, dto: PagamentoRequestDTO) => java.post<LancamentoResponseDTO>(`/lancamentos/caixa/pagamentos/registrar/${id}`, dto),
  desfazerPagamento: (id: number) => java.post<LancamentoResponseDTO>(`/lancamentos/caixa/pagamentos/desfazer/${id}`),

  criarTransferencia: (dto: TransferenciaRequestDTO) => java.post<LancamentoResponseDTO>("/lancamentos/caixa/transferencias/criar", dto),
  atualizarTransferencia: ({ id, ...dto }: UpdateTransferenciaRequestDTO) =>
    java.put<LancamentoResponseDTO>(`/lancamentos/caixa/transferencias/atualizar/${id}`, dto),
  classificar: (dto: ClassificarRequestDTO) => java.put<{ classificados: number }>("/lancamentos/caixa/classificar", dto),
  salvarSaldos: (dto: SaldosRequestDTO) => java.put<void>("/lancamentos/caixa/saldos", dto),
  salvarRateio: (dto: RateioRequestDTO) => java.put<void>("/lancamentos/caixa/rateio", dto),

  criarCategoria: (dto: CreateCategoriaRequestDTO) => java.post<CategoriaResponseDTO>("/api/categoriafinanceira/criar", dto),
  atualizarCategoria: ({ id, ...dto }: UpdateCategoriaRequestDTO) => java.put<CategoriaResponseDTO>(`/api/categoriafinanceira/atualizar/${id}`, dto),
  apagarCategoria: (id: number) => java.delete(`/api/categoriafinanceira/apagar/${id}`),
};
