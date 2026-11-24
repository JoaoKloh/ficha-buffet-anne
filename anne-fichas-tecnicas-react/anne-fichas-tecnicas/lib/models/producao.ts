import type { ItemPratoResponseDTO } from "./prato";

/** Espelha ProducaoResponseDTO.java (GET /producao/retornarTodos): eventos para associar um prato do catálogo. */
export interface ProducaoResponseDTO {
  id: number;
  nome: string;
  quantidade: number;
  data: string; // LocalDate (ISO "AAAA-MM-DD")
  pratos: ItemPratoResponseDTO[];
}
