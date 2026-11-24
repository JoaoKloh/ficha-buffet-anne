import { z } from "zod";

/** Pedaços de schema compartilhados pelos contratos do Anne Gestão. */

export const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/** "AAAA-MM-DD" (LocalDate). */
export const dataIso = (msg = "Informe a data.") => z.string().regex(DATE_RE, msg);
/** "AAAA-MM-DD" ou string vazia (campo de data opcional em formulário). */
export const dataIsoOuVazia = () => z.string().refine((v) => v === "" || DATE_RE.test(v), "Data inválida.");
/** Valor monetário com até 2 casas, maior ou igual a zero. */
export const dinheiro = (msg = "Informe o valor.") =>
  z
    .number({ message: msg })
    .min(0, "O valor não pode ser negativo.")
    .refine((v) => Math.round(v * 100) / 100 === v, "O valor deve ter no máximo 2 casas decimais.");
export const dinheiroPositivo = (msg = "Informe o valor.") =>
  z
    .number({ message: msg })
    .positive("O valor deve ser maior que zero.")
    .refine((v) => Math.round(v * 100) / 100 === v, "O valor deve ter no máximo 2 casas decimais.");
export const idPositivo = (msg = "Id obrigatório.") => z.number({ message: msg }).int().positive(msg);
export const texto = (max: number, msg?: string) => z.string().trim().max(max, msg ?? `Máximo de ${max} caracteres.`);
