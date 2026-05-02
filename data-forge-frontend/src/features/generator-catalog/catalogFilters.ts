import { GeneratorDefinition, GeneratorFilters } from "@/features/generator-catalog/generatorCatalogTypes";

export function filterGenerators(generators: GeneratorDefinition[], filters: GeneratorFilters) {
  const query = filters.query.trim().toLowerCase();

  return generators.filter((generator) => {
    const matchesCategory = filters.category === "All" || generator.category === filters.category;
    const searchableText = [
      generator.id,
      generator.name,
      generator.category,
      generator.summary,
      generator.sample,
      ...generator.params.map((param) => `${param.name} ${param.description}`),
    ]
      .join(" ")
      .toLowerCase();

    return matchesCategory && (query.length === 0 || searchableText.includes(query));
  });
}
