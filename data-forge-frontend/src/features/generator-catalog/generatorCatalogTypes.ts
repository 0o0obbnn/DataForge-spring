export interface GeneratorParam {
  name: string;
  type: "string" | "number" | "boolean" | "select";
  required: boolean;
  description: string;
  options?: string[];
}

export interface GeneratorDefinition {
  id: string;
  name: string;
  category: string;
  summary: string;
  sample: string;
  params: GeneratorParam[];
}

export interface GeneratorFilters {
  query: string;
  category: string;
}
