import { z } from "zod";
import { dinheiro, texto } from "./comum";
import { FUNCOES_EQUIPE, type FuncaoEquipe } from "./dominio";

/** Pessoa do cadastro da equipe (GET /equipe/retornarTodos), com a função e a diária usadas na escala em lote. */
export interface EquipeResponseDTO {
  id: number;
  nome: string;
  funcaoPadrao: FuncaoEquipe;
  diariaPadrao: number;
  contato: string;
  pix: string;
}

/**
 * Edição do cadastro (PATCH): só os campos enviados mudam. Nome, telefone e Pix valem em todas as
 * escalas; função e diária padrão só nas próximas (as escalas existentes mantêm o combinado).
 */
export const AjustarEquipeRequestSchema = z
  .object({
    nome: texto(120).min(1, "Informe o nome.").optional(),
    funcaoPadrao: z.enum(FUNCOES_EQUIPE).optional(),
    diariaPadrao: dinheiro().optional(),
    contato: texto(40).optional(),
    pix: texto(120).optional(),
  })
  .refine((v) => Object.values(v).some((x) => x !== undefined), "Nada a ajustar.");
export type AjustarEquipeRequestDTO = z.infer<typeof AjustarEquipeRequestSchema>;
