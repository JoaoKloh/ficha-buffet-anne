import { apiClient } from "./client";
import type { Dish, CreateDishInput, UpdateDishInput } from "@/lib/models/dish";

export const dishesApi = {
  list: () => apiClient.get<Dish[]>("/api/dishes"),
  get: (id: string) => apiClient.get<Dish>(`/api/dishes/${id}`),
  create: (input: CreateDishInput) => apiClient.post<Dish>("/api/dishes", input),
  update: (id: string, input: UpdateDishInput) =>
    apiClient.patch<Dish>(`/api/dishes/${id}`, input),
  remove: (id: string) => apiClient.delete<void>(`/api/dishes/${id}`),
};
