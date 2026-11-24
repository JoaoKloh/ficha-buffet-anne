import { apiClient } from "../client";
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

/** Caixa (lançamentos, contas, fluxo, transferências e categorias), via BFF /api/gestao. */
const B = "/api/gestao";
const q = (p: Record<string, string | number | boolean>) =>
  Object.entries(p)
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
    .join("&");

export const telaCaixa = (mes: string, filtro: FiltroCaixa, unidade: FiltroUnidade = "") =>
  apiClient.get<CaixaMesResponseDTO>(`${B}/caixa?${q({ mes, filtro, unidade })}`);
export const telaContas = (tipo: "pagar" | "receber", filtro: FiltroContas, unidade: FiltroUnidade = "") =>
  apiClient.get<ContasResponseDTO>(`${B}/caixa/contas?${q({ tipo, filtro, unidade })}`);
export const telaFluxo = (unidade: FiltroUnidade = "") => apiClient.get<FluxoCaixaResponseDTO>(`${B}/caixa/fluxo?${q({ unidade })}`);
export const telaTransferencias = (unidade: FiltroUnidade = "") =>
  apiClient.get<TransferenciasResponseDTO>(`${B}/caixa/transferencias?${q({ unidade })}`);
export const telaSemUnidade = () => apiClient.get<SemUnidadeResponseDTO>(`${B}/caixa/sem-unidade`);

export const obterLancamento = (id: number) => apiClient.get<LancamentoListaItemDTO>(`${B}/lancamentos?id=${id}`);
export const criarLancamento = (dto: CreateLancamentoRequestDTO) => apiClient.post<LancamentoResponseDTO>(`${B}/lancamentos`, dto);
export const atualizarLancamento = (dto: UpdateLancamentoRequestDTO) => apiClient.put<LancamentoResponseDTO>(`${B}/lancamentos`, dto);
export const excluirLancamento = (id: number) => apiClient.delete<void>(`${B}/lancamentos?id=${id}`);
/** Paga ou recebe hoje o saldo inteiro. */
export const marcarPago = (id: number) => apiClient.post<{ ok: true }>(`${B}/lancamentos/${id}/pagar`, {});
export const registrarPagamento = (id: number, dto: PagamentoRequestDTO) => apiClient.post<LancamentoResponseDTO>(`${B}/lancamentos/${id}/pagamentos`, dto);
export const desfazerPagamento = (id: number) => apiClient.post<LancamentoResponseDTO>(`${B}/lancamentos/${id}/pagamentos/desfazer`, {});

export const criarTransferencia = (dto: TransferenciaRequestDTO) => apiClient.post<LancamentoResponseDTO>(`${B}/caixa/transferencias`, dto);
export const atualizarTransferencia = (dto: UpdateTransferenciaRequestDTO) => apiClient.put<LancamentoResponseDTO>(`${B}/caixa/transferencias`, dto);
export const classificarUnidade = (dto: ClassificarRequestDTO) => apiClient.put<{ classificados: number }>(`${B}/caixa/classificar`, dto);
export const salvarSaldos = (dto: SaldosRequestDTO) => apiClient.put<{ ok: true }>(`${B}/caixa/saldos`, dto);
export const salvarRateio = (dto: RateioRequestDTO) => apiClient.put<{ ok: true }>(`${B}/caixa/rateio`, dto);

export const criarCategoria = (dto: CreateCategoriaRequestDTO) => apiClient.post<CategoriaResponseDTO>(`${B}/categorias`, dto);
export const atualizarCategoria = (dto: UpdateCategoriaRequestDTO) => apiClient.put<CategoriaResponseDTO>(`${B}/categorias`, dto);
export const excluirCategoria = (id: number) => apiClient.delete<void>(`${B}/categorias?id=${id}`);

export const telaDre = (ano: string, periodo: string, previstos: boolean, unidade: UnidadeDre = "", regime: RegimeDre = "competencia", ratear = true) =>
  apiClient.get<DreResponseDTO>(`${B}/dre?${q({ ano, periodo, previstos: previstos ? 1 : 0, unidade, regime, ratear: ratear ? 1 : 0 })}`);
