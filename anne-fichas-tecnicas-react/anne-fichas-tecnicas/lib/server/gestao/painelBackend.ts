import { java } from "./java";
import type { PainelResumoResponseDTO } from "@/lib/models/painel";

/** Painel: PainelController (/painel) do backend Java. */
export const painelBackend = {
  resumo: () => java.get<PainelResumoResponseDTO>("/painel/resumo"),
};
