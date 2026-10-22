import { z } from "zod";

export const TIPOS_LANCAMENTO = ["receita", "despesa"] as const;
export type TipoLancamento = (typeof TIPOS_LANCAMENTO)[number];

/**
 * Plano de contas = linhas da DRE (o mesmo do caixa "Caixa e DRE" usado pela Anne).
 * Cada grupo junta categorias; `tipo` diz se a categoria é lançada como receita ou despesa.
 */
export type GrupoDre = "rb" | "ded" | "cd" | "dv" | "df" | "oth";

export interface CategoriaLancamento {
  nome: string;
  tipo: TipoLancamento;
  grupo: GrupoDre;
  dica?: string;
}

export const GRUPOS_DRE: Record<GrupoDre, string> = {
  rb: "Receita bruta",
  ded: "Deduções",
  cd: "Custo direto dos eventos",
  dv: "Despesas variáveis",
  df: "Despesas fixas",
  oth: "Depreciação, financeiro e IR",
};

export const PLANO_DE_CONTAS: readonly CategoriaLancamento[] = [
  { grupo: "rb", tipo: "receita", nome: "Eventos sociais", dica: "casamentos, 15 anos, aniversários" },
  { grupo: "rb", tipo: "receita", nome: "Eventos corporativos", dica: "coffee breaks, almoços" },
  { grupo: "rb", tipo: "receita", nome: "Serviços extras", dica: "bar, doces, bolo, locações" },
  { grupo: "ded", tipo: "despesa", nome: "Impostos sobre vendas", dica: "DAS, ISS, PIS, COFINS" },
  { grupo: "ded", tipo: "despesa", nome: "Descontos e cancelamentos" },
  { grupo: "cd", tipo: "despesa", nome: "Alimentos e bebidas", dica: "insumos" },
  { grupo: "cd", tipo: "despesa", nome: "Mão de obra de evento", dica: "garçons, cozinheiros, maîtres" },
  { grupo: "cd", tipo: "despesa", nome: "Descartáveis, embalagens e gás" },
  { grupo: "cd", tipo: "despesa", nome: "Locação de louças e mobiliário" },
  { grupo: "cd", tipo: "despesa", nome: "Transporte e frete" },
  { grupo: "dv", tipo: "despesa", nome: "Comissões", dica: "cerimonialistas, indicações" },
  { grupo: "dv", tipo: "despesa", nome: "Taxas de cartão e antecipação" },
  { grupo: "dv", tipo: "despesa", nome: "Degustações" },
  { grupo: "df", tipo: "despesa", nome: "Aluguel da cozinha / fábrica" },
  { grupo: "df", tipo: "despesa", nome: "Água, luz e internet" },
  { grupo: "df", tipo: "despesa", nome: "Salários fixos + encargos" },
  { grupo: "df", tipo: "despesa", nome: "Pró-labore" },
  { grupo: "df", tipo: "despesa", nome: "Contador e sistemas" },
  { grupo: "df", tipo: "despesa", nome: "Marketing", dica: "Instagram, anúncios, fotos" },
  { grupo: "df", tipo: "despesa", nome: "Manutenção, seguros e alvarás" },
  { grupo: "oth", tipo: "despesa", nome: "Depreciação" },
  { grupo: "oth", tipo: "receita", nome: "Receitas financeiras", dica: "rendimentos" },
  { grupo: "oth", tipo: "despesa", nome: "Despesas financeiras", dica: "juros, tarifas" },
  { grupo: "oth", tipo: "despesa", nome: "IRPJ / CSLL", dica: "só Lucro Presumido ou Real" },
];

export const CATEGORIAS_POR_TIPO: Record<TipoLancamento, readonly string[]> = {
  receita: PLANO_DE_CONTAS.filter((c) => c.tipo === "receita").map((c) => c.nome),
  despesa: PLANO_DE_CONTAS.filter((c) => c.tipo === "despesa").map((c) => c.nome),
};

/** Categorias da versão anterior do Financeiro, para lançamentos antigos caírem na linha certa da DRE. */
const GRUPO_CATEGORIA_LEGADA: Record<string, GrupoDre> = {
  "Evento fechado": "rb",
  "Degustação": "rb",
  "Sinal/Entrada": "rb",
  "Serviço extra": "rb",
  "Outra receita": "rb",
  "Insumos/Alimentos": "cd",
  "Bebidas": "cd",
  "Pessoal/Equipe": "cd",
  "Transporte": "cd",
  "Impostos": "ded",
  "Aluguel/Espaço": "df",
  "Equipamentos": "df",
  "Outra despesa": "df",
};

/** Grupo da DRE de uma categoria; desconhecida cai em Receita bruta ou Despesas fixas. */
export function grupoDaCategoria(categoria: string, tipo: TipoLancamento): GrupoDre {
  return (
    PLANO_DE_CONTAS.find((c) => c.nome === categoria)?.grupo ??
    GRUPO_CATEGORIA_LEGADA[categoria] ??
    (tipo === "receita" ? "rb" : "df")
  );
}

/** Categorias do tipo (ou todas) agrupadas como no menu: [rótulo do grupo, categorias]. */
export function categoriasAgrupadas(tipo?: TipoLancamento): [string, CategoriaLancamento[]][] {
  const grupos = new Map<GrupoDre, CategoriaLancamento[]>();
  for (const c of PLANO_DE_CONTAS) {
    if (tipo && c.tipo !== tipo) continue;
    grupos.set(c.grupo, [...(grupos.get(c.grupo) ?? []), c]);
  }
  return Array.from(grupos, ([g, cs]) => [GRUPOS_DRE[g], cs]);
}

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
  data: string; // LocalDate (ISO "AAAA-MM-DD") — data inicial
  dataFinal: string; // LocalDate (ISO "AAAA-MM-DD")
  producaoIds: number[]; // produções vinculadas (N:N), pode ser vazio
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
  dataFinal: z.string().regex(DATE_RE, "A data final do lançamento é obrigatória."),
  // Opcional (vazio = nenhuma). O backend responde 404 se alguma produção não existir.
  producaoIds: z.array(z.number({ message: "O id da produção não pode ser nulo." }).int().positive()),
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
