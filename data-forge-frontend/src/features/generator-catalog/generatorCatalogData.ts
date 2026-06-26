import { GeneratorDefinition } from "@/features/generator-catalog/generatorCatalogTypes";

export const generatorCatalog: GeneratorDefinition[] = [
  {
    id: "uuid",
    name: "UUID",
    category: "Identity",
    summary: "Generate RFC-style unique identifiers for primary keys and correlation IDs.",
    sample: "550e8400-e29b-41d4-a716-446655440000",
    params: [{ name: "version", type: "select", required: false, description: "UUID version.", options: ["v4"] }],
  },
  {
    id: "string",
    name: "String",
    category: "Core",
    summary: "Generate bounded random strings for names, labels, and free-form values.",
    sample: "alpha-8291",
    params: [
      { name: "minLength", type: "number", required: false, description: "Minimum generated length." },
      { name: "maxLength", type: "number", required: false, description: "Maximum generated length." },
    ],
  },
  {
    id: "integer",
    name: "Integer",
    category: "Core",
    summary: "Generate integer values for counters, quantities, and numeric dimensions.",
    sample: "4287",
    params: [
      { name: "min", type: "number", required: false, description: "Minimum value." },
      { name: "max", type: "number", required: false, description: "Maximum value." },
    ],
  },
  {
    id: "decimal",
    name: "Decimal",
    category: "Core",
    summary: "Generate decimal numbers for prices, measurements, and ratios.",
    sample: "128.45",
    params: [{ name: "scale", type: "number", required: false, description: "Number of decimal places." }],
  },
  {
    id: "boolean",
    name: "Boolean",
    category: "Core",
    summary: "Generate true/false values for flags and status columns.",
    sample: "true",
    params: [{ name: "trueRate", type: "number", required: false, description: "Probability of true values." }],
  },
  {
    id: "date",
    name: "Date",
    category: "Date/Time",
    summary: "Generate calendar dates within optional bounds.",
    sample: "2026-04-26",
    params: [
      { name: "start", type: "string", required: false, description: "Earliest date." },
      { name: "end", type: "string", required: false, description: "Latest date." },
    ],
  },
  {
    id: "timestamp",
    name: "Timestamp",
    category: "Date/Time",
    summary: "Generate precise timestamps for event streams and audit records.",
    sample: "2026-04-26T15:20:00Z",
    params: [{ name: "timezone", type: "string", required: false, description: "Output timezone." }],
  },
  {
    id: "name",
    name: "Name",
    category: "Identity",
    summary: "Generate realistic personal names for customer or employee datasets.",
    sample: "Lin Wei",
    params: [{ name: "locale", type: "string", required: false, description: "Name locale preference." }],
  },
  {
    id: "email",
    name: "Email",
    category: "Internet",
    summary: "Generate email addresses for account and contact datasets.",
    sample: "user@example.com",
    params: [{ name: "domain", type: "string", required: false, description: "Fixed email domain." }],
  },
  {
    id: "phone",
    name: "Phone",
    category: "Identity",
    summary: "Generate phone numbers for contact records.",
    sample: "+86 138 0000 1234",
    params: [{ name: "country", type: "string", required: false, description: "Country or region code." }],
  },
  {
    id: "idcard",
    name: "ID Card",
    category: "Identity",
    summary: "Generate identity card-like values for regulated test datasets.",
    sample: "110101199001011234",
    params: [{ name: "masked", type: "boolean", required: false, description: "Mask sensitive segments." }],
  },
  {
    id: "address",
    name: "Address",
    category: "Address",
    summary: "Generate postal addresses with city, street, and region information.",
    sample: "88 Century Avenue, Shanghai",
    params: [{ name: "country", type: "string", required: false, description: "Country or region." }],
  },
  {
    id: "bankcard",
    name: "Bank Card",
    category: "Finance",
    summary: "Generate bank card-like numbers suitable for non-production tests.",
    sample: "622202*********1234",
    params: [{ name: "issuer", type: "string", required: false, description: "Card issuer hint." }],
  },
  {
    id: "ip",
    name: "IP Address",
    category: "Internet",
    summary: "Generate IPv4 or IPv6 addresses for networking datasets.",
    sample: "192.168.10.24",
    params: [{ name: "version", type: "select", required: false, description: "IP version.", options: ["v4", "v6"] }],
  },
  {
    id: "mac",
    name: "MAC Address",
    category: "Internet",
    summary: "Generate hardware address values for device inventories.",
    sample: "00:1A:2B:3C:4D:5E",
    params: [{ name: "separator", type: "select", required: false, description: "Address separator.", options: [":", "-"] }],
  },
  {
    id: "url",
    name: "URL",
    category: "Internet",
    summary: "Generate URLs for crawler, analytics, and content datasets.",
    sample: "https://dataforge.example/jobs/42",
    params: [{ name: "protocol", type: "select", required: false, description: "URL protocol.", options: ["https", "http"] }],
  },
  {
    id: "domain",
    name: "Domain",
    category: "Internet",
    summary: "Generate domain names for account, DNS, and web test data.",
    sample: "example.org",
    params: [{ name: "tld", type: "string", required: false, description: "Top-level domain." }],
  },
  {
    id: "company",
    name: "Company",
    category: "Company",
    summary: "Generate company names for CRM and B2B datasets.",
    sample: "Northwind Data Systems",
    params: [{ name: "suffix", type: "boolean", required: false, description: "Include legal suffix." }],
  },
  {
    id: "vehicle",
    name: "Vehicle",
    category: "Vehicle",
    summary: "Generate vehicle identifiers and model-like values.",
    sample: "EV-DF-2026",
    params: [{ name: "type", type: "string", required: false, description: "Vehicle type hint." }],
  },
  {
    id: "yaml",
    name: "YAML",
    category: "Custom",
    summary: "Generate YAML-shaped snippets for configuration-oriented test cases.",
    sample: "enabled: true",
    params: [{ name: "template", type: "string", required: false, description: "YAML structure template." }],
  },
];

export function mergeWithBackendData(
  staticCatalog: GeneratorDefinition[],
  backendGenerators: { id: string; name: string; description: string }[] | undefined,
): GeneratorDefinition[] {
  if (!Array.isArray(backendGenerators)) {
    return staticCatalog;
  }
  if (backendGenerators.length === 0) {
    return staticCatalog;
  }

  const staticMap = new Map(staticCatalog.map((g) => [g.id, g]));
  const merged: GeneratorDefinition[] = [];

  for (const backend of backendGenerators) {
    const staticGen = staticMap.get(backend.id);
    if (staticGen) {
      merged.push(staticGen);
    } else {
      merged.push({
        id: backend.id,
        name: backend.name,
        category: "Core",
        summary: backend.description,
        sample: backend.name,
        params: [],
      });
    }
  }

  return merged;
}

export function getAllCategories(generators: GeneratorDefinition[]): string[] {
  const categories = new Set(generators.map((g) => g.category));
  return ["All", ...Array.from(categories).sort()];
}

export const generatorCategories = [
  "All",
  ...Array.from(new Set(generatorCatalog.map((generator) => generator.category))).sort(),
];
