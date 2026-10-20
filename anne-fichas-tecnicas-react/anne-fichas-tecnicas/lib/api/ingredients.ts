// lib/api/ingredients.ts
import { apiClient } from "./client";
import type {
  IngredienteResponseDTO,
  CreateIngredienteRequestDTO,
  UpdateIngredienteRequestDto,
} from "@/lib/models/ingredient";

export const ingredientsApi = {
  list: () => apiClient.get<IngredienteResponseDTO[]>("/api/ingredients"),
  // O backend responde 201 sem corpo (sem id) — ver ingredientBackend.criar.
  create: (input: CreateIngredienteRequestDTO) => apiClient.post<void>("/api/ingredients", input),
  update: (id: number, input: UpdateIngredienteRequestDto) =>
    apiClient.put<IngredienteResponseDTO>("/api/ingredients", input),
  remove: (id: number) => apiClient.delete<void>(`/api/ingredients/${id}`),
};