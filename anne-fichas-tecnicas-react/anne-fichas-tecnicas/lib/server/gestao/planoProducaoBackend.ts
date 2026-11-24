import { java, query } from "./java";
import type { UnidadePlano } from "@/lib/models/dominio";
import type {
  AdicionarItensPlanoRequestDTO,
  CalcularCartaRequestDTO,
  PlanoDetalheResponseDTO,
  PlanoProducaoResponseDTO,
  PlanosProducaoResponseDTO,
  SalvarPlanoRequestDTO,
} from "@/lib/models/planoProducao";

/**
 * Planos de produção do restaurante e da chocolates: PlanoProducaoController (/planosProducao) do backend Java,
 * ainda a criar (tabela própria, que não pode se chamar `producoes`).
 */
const P = "/planosProducao";
const k = (s: string) => encodeURIComponent(s);

export const planoProducaoBackend = {
  listar: (unidade: UnidadePlano) => java.get<PlanosProducaoResponseDTO>(`${P}/retornarTodos${query({ unidade })}`),
  detalhe: (id: number) => java.get<PlanoDetalheResponseDTO>(`${P}/detalhar/${id}`),
  criar: (dto: SalvarPlanoRequestDTO) => java.post<PlanoProducaoResponseDTO>(`${P}/criar`, dto),
  atualizar: (id: number, dto: SalvarPlanoRequestDTO) => java.put<PlanoProducaoResponseDTO>(`${P}/atualizar/${id}`, dto),
  apagar: (id: number) => java.delete(`${P}/apagar/${id}`),

  adicionarItens: (id: number, dto: AdicionarItensPlanoRequestDTO) => java.post<PlanoProducaoResponseDTO>(`${P}/itens/adicionar/${id}`, dto),
  ajustarItem: (id: number, item: string, qtd: number) => java.patch<PlanoProducaoResponseDTO>(`${P}/itens/ajustar/${id}/${k(item)}`, { qtd }),
  removerItem: (id: number, item: string) => java.delete(`${P}/itens/apagar/${id}/${k(item)}`),
  concluirItem: (id: number, item: string) => java.post<{ loteId: number; codigo: string }>(`${P}/itens/concluir/${id}/${k(item)}`),
  desfazerItem: (id: number, item: string) => java.post<void>(`${P}/itens/desfazer/${id}/${k(item)}`),
  calcularCarta: (id: number, dto: CalcularCartaRequestDTO) => java.post<{ fichas: number }>(`${P}/calcularCarta/${id}`, dto),
};
