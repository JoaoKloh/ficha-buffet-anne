// app/ingredientes/page.tsx
import { IngredientListView } from "@/components/IngredientListView";
import { ingredientBackend } from "@/lib/server/ingredientBackend";
import type { IngredienteResponseDTO } from "@/lib/models/ingredient";

// Renderiza sempre no servidor para ter os ingredientes atualizados a cada requisição
export const dynamic = "force-dynamic";

export default async function IngredientesPage() {
  let ingredientes: IngredienteResponseDTO[];

  try {
    // Busca a lista de ingredientes no Spring Boot via servidor Node.js
    ingredientes = await ingredientBackend.listarTodos();
  } catch (err) {
    console.error("[IngredientesPage] Falha ao carregar ingredientes do backend", err);
    ingredientes = [];
  }

  // Passa os dados iniciais para o componente do cliente
  return <IngredientListView initialIngredients={ingredientes} />;
}