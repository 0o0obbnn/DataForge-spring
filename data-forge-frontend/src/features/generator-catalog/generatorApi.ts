import { apiRequest } from "@/shared/api/apiClient";
import { GeneratorDefinition } from "@/features/generator-catalog/generatorCatalogTypes";

export interface BackendGenerator {
  id: string;
  name: string;
  description: string;
}

export function listGenerators() {
  return apiRequest<BackendGenerator[]>("/api/v1/dataforge/generators");
}
