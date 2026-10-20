import { apiClient } from "./client";
import type {
  Production,
  CreateProductionInput,
  UpdateProductionInput,
} from "@/lib/models/production";

export const productionsApi = {
  list: () => apiClient.get<Production[]>("/api/productions"),
  get: (id: string) => apiClient.get<Production>(`/api/productions/${id}`),
  create: (input: CreateProductionInput) =>
    apiClient.post<Production>("/api/productions", input),
  update: (id: string, input: UpdateProductionInput) =>
    apiClient.patch<Production>(`/api/productions/${id}`, input),
  remove: (id: string) => apiClient.delete<void>(`/api/productions/${id}`),
};
