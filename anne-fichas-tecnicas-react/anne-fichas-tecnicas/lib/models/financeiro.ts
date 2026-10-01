import { z } from "zod";

export const TIPOS_LANCAMENTO = ["receita", "despesa"] as const;
export type TipoLancamento = (typeof TIPOS_LANCAMENTO)[number];

export const CATEGORIAS_RECEITA = [
  "Evento fechado",
  "Degustação",
  "Sinal/Entrada",
  "Serviço extra",
  "Outra receita",
] as const;

export const CATEGORIAS_DESPESA = [
  "Insumos/Alimentos",
  "Bebidas",
  "Pessoal/Equipe",
  "Aluguel/Espaço",
  "Marketing",
  "Equipamentos",
  "Transporte",
  "Impostos",
  "Outra despesa",
] as const;

export const CATEGORIAS_POR_TIPO = {
  receita: CATEGORIAS_RECEITA,
  despesa: CATEGORIAS_DESPESA,
} as const satisfies Record<TipoLancamento, readonly string[]>;

/**
 * Tipos espelhando, campo a campo, os DTOs de lançamento do backend real
 * (LancamentoController.java / LancamentoResponseDTO.java).
 */
export interface LancamentoResponseDTO {
  id: number;
  tipo: TipoLancamento;
  categoria: string;
  descricao: string;
  valor: number; // BigDecimal (2 casas decimais)
  data: string; // LocalDate (ISO "AAAA-MM-DD")
}

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Espelha CriarLancamentoDTO.java (@Pattern, @NotBlank, @Size, @Positive, @Digits, @NotNull).
 */
export const CriarLancamentoSchema = z.object({
  tipo: z.enum(TIPOS_LANCAMENTO, {
    message: "O tipo do lançamento deve ser 'receita' ou 'despesa'.",
  }),
  categoria: z
    .string()
    .trim()
    .min(1, "A categoria do lançamento é obrigatória.")
    .max(100, "A categoria deve ter no máximo 100 caracteres."),
  descricao: z
    .string()
    .trim()
    .min(1, "A descrição do lançamento é obrigatória.")
    .max(255, "A descrição deve ter no máximo 255 caracteres."),
  valor: z
    .number({ message: "O valor do lançamento é obrigatório." })
    .positive("O valor do lançamento deve ser maior que zero.")
    .refine((v) => Math.round(v * 100) / 100 === v, "O valor deve ter no máximo 2 casas decimais."),
  data: z.string().regex(DATE_RE, "A data do lançamento é obrigatória."),
});
export type CriarLancamentoDTO = z.infer<typeof CriarLancamentoSchema>;

/**
 * Espelha AtualizarLancamentoDTO.java.
 */
export const AtualizarLancamentoSchema = CriarLancamentoSchema.extend({
  id: z.number({ message: "O id do lançamento não pode ser nulo." }),
});
export type AtualizarLancamentoDTO = z.infer<typeof AtualizarLancamentoSchema>;

// Nomes usados pelas telas do Financeiro.
export type Lancamento = LancamentoResponseDTO;
export type NovoLancamento = CriarLancamentoDTO;
