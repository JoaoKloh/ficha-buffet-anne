import { apiClient } from "../client";
import type { AjustarEquipeRequestDTO, EquipeResponseDTO } from "@/lib/models/equipe";

/** Cadastro da equipe, via BFF /api/gestao/equipe. */
export const listarEquipe = () => apiClient.get<EquipeResponseDTO[]>("/api/gestao/equipe");
export const ajustarEquipe = (id: number, dto: AjustarEquipeRequestDTO) => apiClient.patch<EquipeResponseDTO>(`/api/gestao/equipe/${id}`, dto);
