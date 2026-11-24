import { java } from "./java";
import type { AjustarEquipeRequestDTO, EquipeResponseDTO } from "@/lib/models/equipe";

/** Cadastro da equipe: EquipeController (/equipe) do backend Java. A escala de cada evento fica em eventoBackend. */
export const equipeBackend = {
  listar: () => java.get<EquipeResponseDTO[]>("/equipe/retornarTodos"),
  ajustar: (id: number, dto: AjustarEquipeRequestDTO) => java.patch<EquipeResponseDTO>(`/equipe/ajustar/${id}`, dto),
};
