import { apiClient } from "../client";
import type {
  EstoqueComprasResponseDTO,
  EstoqueConferenciaResponseDTO,
  EstoquePerdasResponseDTO,
  EstoqueValidadesResponseDTO,
  MarcarPedidoRequestDTO,
  CreateLoteRequestDTO,
  CreateMovimentoRequestDTO,
  EstoqueLoucasResponseDTO,
  FormOpcoesEstoque,
  OpcoesEstoqueDe,
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
import { normalizarCompras, normalizarConferencia, normalizarPerdas, normalizarValidades } from "@/lib/models/estoque";
import type { HorizonteCompra } from "@/lib/models/dominio";

/** Estoque, lotes e notas fiscais, via BFF /api/gestao. */
const B = "/api/gestao";
/** Opções de um formulário: cada um pede só os campos que usa. */
export const opcoesEstoque = <F extends FormOpcoesEstoque>(para: F) => apiClient.get<OpcoesEstoqueDe[F]>(`${B}/estoque/opcoes?para=${para}`);
export const telaSaldo = () => apiClient.get<EstoqueSaldoResponseDTO>(`${B}/estoque/saldo`);
export const itemEstoque = (id: number) => apiClient.get<ItemEstoqueDetalheDTO>(`${B}/estoque/itens/${id}`);
export const telaLoucas = (data: string | null) => apiClient.get<EstoqueLoucasResponseDTO>(`${B}/estoque/loucas${data ? `?data=${data}` : ""}`);
/* Estoque integrado: a resposta é normalizada para as abas mostrarem todos os blocos mesmo com campos nulos. */
export const telaCompras = async (horizonte: HorizonteCompra) =>
  normalizarCompras(await apiClient.get<Partial<EstoqueComprasResponseDTO>>(`${B}/estoque/compras?horizonte=${horizonte}`), horizonte);
export const marcarPedido = (dto: MarcarPedidoRequestDTO) => apiClient.put<{ ok: true }>(`${B}/estoque/compras/pedido`, dto);
export const telaValidades = async () => normalizarValidades(await apiClient.get<Partial<EstoqueValidadesResponseDTO>>(`${B}/estoque/validades`));
export const telaPerdas = async (mes: string) => normalizarPerdas(await apiClient.get<Partial<EstoquePerdasResponseDTO>>(`${B}/estoque/perdas?mes=${mes}`), mes);
export const telaConferencia = async () => normalizarConferencia(await apiClient.get<Partial<EstoqueConferenciaResponseDTO>>(`${B}/estoque/conferencia`));
export const telaPreparos = (filtro: string) => apiClient.get<EstoquePreparosResponseDTO>(`${B}/estoque/preparos?filtro=${filtro}`);

export const listarMovimentos = (filtro: string) => apiClient.get<MovimentosResponseDTO>(`${B}/movimentos?filtro=${filtro}`);
export const criarMovimento = (dto: CreateMovimentoRequestDTO) => apiClient.post<MovimentoResponseDTO>(`${B}/movimentos`, dto);
export const excluirMovimento = (id: number) => apiClient.delete<void>(`${B}/movimentos?id=${id}`);
export const lancarInventario = (dto: InventarioRequestDTO) => apiClient.post<{ lancados: number }>(`${B}/movimentos/inventario`, dto);

export const obterLote = (id: number) => apiClient.get<LoteListaItemDTO>(`${B}/lotes?id=${id}`);
export const criarLote = (dto: CreateLoteRequestDTO) => apiClient.post<LoteResponseDTO>(`${B}/lotes`, dto);
export const atualizarLote = (id: number, dto: CreateLoteRequestDTO | Omit<UpdateLoteRequestDTO, "id">) => apiClient.put<LoteResponseDTO>(`${B}/lotes?id=${id}`, dto);
export const excluirLote = (id: number) => apiClient.delete<void>(`${B}/lotes?id=${id}`);
export const movimentarLote = (id: number, dto: MovimentarLoteRequestDTO) => apiClient.post<LoteResponseDTO>(`${B}/lotes/${id}/movimentar`, dto);

export const listarNfs = () => apiClient.get<NotaFiscalListaItemDTO[]>(`${B}/nfs`);
export const obterNf = (id: number) => apiClient.get<NotaFiscalListaItemDTO>(`${B}/nfs?id=${id}`);
export const salvarRascunhoNf = (id: number | null, dto: SalvarNotaFiscalRequestDTO) =>
  id ? apiClient.put<NotaFiscalResponseDTO>(`${B}/nfs?id=${id}`, dto) : apiClient.post<NotaFiscalResponseDTO>(`${B}/nfs`, dto);
export const lancarNf = (id: number | null, dto: SalvarNotaFiscalRequestDTO) => apiClient.post<NotaFiscalResponseDTO>(`${B}/nfs/lancar${id ? `?id=${id}` : ""}`, dto);
export const estornarNf = (id: number) => apiClient.post<{ ok: true }>(`${B}/nfs/${id}/estornar`, {});
export const excluirNf = (id: number) => apiClient.delete<void>(`${B}/nfs?id=${id}`);
export const importarXmlNf = (xml: string) => apiClient.post<NotaFiscalImportadaDTO>(`${B}/nfs/importar-xml`, { xml });
export const deParaNf = (dto: NfDeParaRequestDTO) => apiClient.post<({ insumoId: number; unid: "estoque" | "compra" } | null)[]>(`${B}/nfs/depara`, dto);
/** Interface pronta; o servidor responde 501 até a leitura por foto existir. */
export const lerNfPorFoto = () => apiClient.post<NotaFiscalImportadaDTO>(`${B}/nfs/ler-foto`, {});
