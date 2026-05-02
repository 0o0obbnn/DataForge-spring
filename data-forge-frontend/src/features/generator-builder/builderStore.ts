import { create } from "zustand";

export interface BuilderField {
  name: string;
  type: string;
  params?: Record<string, unknown>;
}

export interface BuilderDraft {
  count: number;
  threads?: number;
  validate: boolean;
  seed?: number;
  output: {
    format: "CSV" | "JSON" | "SQL" | "CONSOLE";
    file?: string;
    encoding: string;
  };
  fields: BuilderField[];
}

interface BuilderState {
  draft: BuilderDraft;
  addField: (generatorType: string) => void;
  removeField: (index: number) => void;
  duplicateField: (index: number) => void;
  updateField: (index: number, patch: Partial<BuilderField>) => void;
  updateDraft: (patch: Partial<Omit<BuilderDraft, "fields" | "output">>) => void;
  updateOutput: (patch: Partial<BuilderDraft["output"]>) => void;
  resetDraft: () => void;
}

const defaultDraft: BuilderDraft = {
  count: 100,
  threads: 1,
  validate: true,
  output: {
    format: "JSON",
    encoding: "UTF-8",
  },
  fields: [],
};

function createFieldName(generatorType: string, fields: BuilderField[]) {
  const baseName = generatorType.replace(/[^a-zA-Z0-9]+/g, "_").toLowerCase();
  const matchingCount = fields.filter((field) => field.name === baseName || field.name.startsWith(`${baseName}_`)).length;

  return matchingCount === 0 ? baseName : `${baseName}_${matchingCount + 1}`;
}

export const useBuilderStore = create<BuilderState>()((set) => ({
  draft: defaultDraft,
  addField: (generatorType) =>
    set((state) => ({
      draft: {
        ...state.draft,
        fields: [
          ...state.draft.fields,
          {
            name: createFieldName(generatorType, state.draft.fields),
            type: generatorType,
            params: {},
          },
        ],
      },
    })),
  removeField: (index) =>
    set((state) => ({
      draft: {
        ...state.draft,
        fields: state.draft.fields.filter((_, currentIndex) => currentIndex !== index),
      },
    })),
  duplicateField: (index) =>
    set((state) => {
      const field = state.draft.fields[index];

      if (!field) {
        return state;
      }

      return {
        draft: {
          ...state.draft,
          fields: [
            ...state.draft.fields,
            {
              ...field,
              name: createFieldName(field.type, state.draft.fields),
              params: { ...field.params },
            },
          ],
        },
      };
    }),
  updateField: (index, patch) =>
    set((state) => ({
      draft: {
        ...state.draft,
        fields: state.draft.fields.map((field, currentIndex) =>
          currentIndex === index ? { ...field, ...patch } : field,
        ),
      },
    })),
  updateDraft: (patch) =>
    set((state) => ({
      draft: {
        ...state.draft,
        ...patch,
      },
    })),
  updateOutput: (patch) =>
    set((state) => ({
      draft: {
        ...state.draft,
        output: {
          ...state.draft.output,
          ...patch,
        },
      },
    })),
  resetDraft: () =>
    set({
      draft: defaultDraft,
    }),
}));
