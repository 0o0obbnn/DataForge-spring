import { z } from "zod";

export const generateRequestSchema = z
  .object({
    count: z.number().int().min(1).max(1_000_000),
    threads: z.number().int().min(1).max(16).optional(),
    validate: z.boolean().default(true),
    seed: z.number().int().optional(),
    output: z.object({
      format: z.enum(["CSV", "JSON", "SQL", "CONSOLE"]),
      file: z.string().optional(),
      encoding: z.string().default("UTF-8"),
    }),
    fields: z
      .array(
        z.object({
          name: z.string().trim().min(1),
          type: z.string().trim().min(1),
          params: z.record(z.string(), z.unknown()).optional(),
        }),
      )
      .min(1),
  })
  .superRefine((value, context) => {
    const names = new Set<string>();

    value.fields.forEach((field, index) => {
      if (names.has(field.name)) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          message: "Field names must be unique",
          path: ["fields", index, "name"],
        });
      }
      names.add(field.name);
    });
  });

export type GenerateRequestFormValues = z.infer<typeof generateRequestSchema>;
