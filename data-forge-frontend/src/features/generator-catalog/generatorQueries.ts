import { useQuery } from "@tanstack/react-query";

import { listGenerators } from "@/features/generator-catalog/generatorApi";

export const generatorQueryKeys = {
  all: () => ["generators"] as const,
};

export function useGeneratorsQuery() {
  return useQuery({
    queryKey: generatorQueryKeys.all(),
    queryFn: () => listGenerators(),
    staleTime: 60_000,
  });
}
