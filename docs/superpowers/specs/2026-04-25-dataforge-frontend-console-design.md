# DataForge Frontend Console MVP Design

## Status

Approved for planning.

## Goal

Build the first production-ready DataForge web console as a React-based developer tool dashboard. The MVP focuses on making the existing Spring Boot API usable through a polished dark, futuristic interface: authenticate, discover generators, configure data generation jobs, run sync or async generation, track tasks, and reuse templates.

The frontend should feel like a professional data engineering cockpit rather than a generic admin panel.

## Project Context

DataForge is a Java 21 and Spring Boot 3.2 multi-module Maven project. The backend already exposes core web APIs for:

- JWT authentication under `/api/v1/auth`.
- Data generation under `/api/v1/dataforge`.
- Async task status and recent task history under `/api/v1/dataforge/tasks`.
- Template management under `/api/v1/templates`.
- OpenAPI documentation through SpringDoc.

The existing `data-forge-frontend` directory contains only Vite/Vue entry remnants and no complete frontend implementation. The approved direction is to replace that incomplete frontend with a React + Vite console MVP.

## Product Scope

The first release is a control console MVP, not a marketing site or full SaaS portal.

Included pages:

- Login: real JWT login with a development mock mode.
- Dashboard: operating overview for recent generation activity and system status.
- Generator Builder: configure fields, count, output format, validation, seed, and sync or async execution.
- Generator Catalog: searchable and browsable library of available generator types.
- Tasks: async task list, detail, status, duration, and failure reasons.
- Templates: CRUD for reusable generation configurations.

Out of scope for MVP:

- Public landing page.
- Billing, multi-tenant organization management, and user administration.
- Realtime WebSocket progress streaming.
- In-browser generated file preview for very large outputs.
- A full visual workflow designer.

## Technical Selection

Recommended stack:

- React + Vite + TypeScript for a fast SPA developer workflow.
- Tailwind CSS for design tokens and utility-first styling.
- shadcn/ui for accessible, customizable primitives built on Radix UI patterns.
- TanStack Query for server state, request lifecycle, caching, retries, and polling.
- Zustand for small client state: auth session, dev mock flag, layout preferences, and builder draft state.
- React Hook Form + Zod for complex form state and validation in Generator Builder and Template forms.
- React Router for SPA routing.
- Lucide React for consistent SVG iconography.
- Recharts or ECharts for dashboard metrics if charting is needed in MVP.
- Vitest + React Testing Library for unit and component tests.
- Playwright for core end-to-end flows.

This stack keeps the frontend independent from Spring Boot while matching the backend's API-first architecture. Next.js is intentionally not selected for MVP because server rendering and file-based backend routes do not add enough value for this internal console.

## Frontend Architecture

Use feature-oriented organization:

```text
data-forge-frontend/
  src/
    app/
      App.tsx
      router.tsx
      providers.tsx
    pages/
      LoginPage.tsx
      DashboardPage.tsx
      GeneratorBuilderPage.tsx
      GeneratorCatalogPage.tsx
      TasksPage.tsx
      TemplatesPage.tsx
    features/
      auth/
      dashboard/
      generator-builder/
      generator-catalog/
      tasks/
      templates/
    shared/
      api/
      components/
      config/
      lib/
      types/
```

Architecture rules:

- `shared/api` owns the HTTP client, API response normalization, token attachment, and error mapping.
- `features/*/api.ts` owns feature-specific API calls and TanStack Query hooks.
- `features/*/components` owns feature-specific UI blocks.
- `shared/components` owns cross-feature shell components, empty states, loading states, error boundaries, and base layout.
- Forms validate with Zod schemas close to the feature that owns the form.
- API DTO types are maintained as explicit TypeScript interfaces under `shared/types` for MVP and kept aligned with backend models. OpenAPI-based generation is outside the MVP scope.

## API Integration

Development:

- Vite dev server runs on `localhost:5173`.
- Backend runs on `localhost:8080`.
- Vite proxies `/api` to the backend to avoid browser CORS friction during local development.

Production:

- Use `VITE_API_BASE_URL` for backend base URL.
- Token-bearing requests include `Authorization: Bearer <accessToken>`.

Authentication mode:

- Real mode calls `/api/v1/auth/login`, `/api/v1/auth/refresh`, and `/api/v1/auth/logout`.
- Dev mock mode is enabled only when `VITE_ENABLE_MOCK_AUTH=true`.
- Mock auth must be visibly labeled in the UI to avoid confusing development sessions with real authorization.

Error handling:

- Map backend `ApiResponse` errors into typed frontend errors.
- Show validation errors near the affected field.
- Show task and API failures with request id when available.
- For `401`, clear auth state and redirect to Login unless refresh succeeds.
- For `429`, show rate limit messaging and retry guidance.

Polling:

- Tasks page polls recent async tasks while any task is `IN_PROGRESS`.
- Polling backs off when the tab is hidden or no active jobs exist.

## Page Designs

### Login

Purpose: authenticate quickly without distracting from the console.

Layout:

- Split-screen dark hero.
- Left side: DataForge identity, short product promise, animated grid/noise background.
- Right side: compact login card with username, password, submit, dev mock toggle if enabled.

States:

- Loading button during login.
- Inline error for invalid credentials.
- Mock mode badge when active.

### Dashboard

Purpose: show whether DataForge is ready and how generation work is flowing.

Content:

- KPI cards: total recent tasks, completed, failed, average duration, estimated throughput.
- System status card: API health, auth status, backend URL, current mode.
- Recent tasks table.
- Quick actions: New Generation, Browse Catalog, Create Template.

Visual treatment:

- Glass cards over a dark radial-gradient background.
- Neon status dots for healthy, running, failed, and rate-limited states.

### Generator Builder

Purpose: create a generation request with confidence.

Content:

- Top-level controls: count, threads, validate, seed, output format, output file.
- Field list editor: name, generator type, params, validation hints.
- Preview panel: request JSON/YAML preview before submit.
- Execution controls: Run Sync for small jobs, Run Async for large jobs.

Interaction:

- Add field from Generator Catalog.
- Duplicate, reorder, and remove fields.
- Inline validation for missing names, duplicate names, invalid counts, and unsupported output formats.
- After async submission, route or link to task detail.

### Generator Catalog

Purpose: make the 60+ generator ecosystem discoverable.

Content:

- Search by name, category, output type, or common use case.
- Category filters such as Identity, Internet, Finance, Address, Date/Time, Vehicle, Company, and Custom.
- Bento-grid generator cards with name, summary, sample output, and parameter count.
- Detail drawer with parameters, examples, constraints, and `Add to Builder`.

Interaction:

- Keyboard-friendly command palette style search.
- Favorites and recent generator shortcuts are outside the MVP scope.

### Tasks

Purpose: monitor async generation work.

Content:

- Task table with id, status, record count, duration, created/completed times, error message.
- Status filters and search by task id.
- Detail drawer for task metadata and config snapshot when available.

Interaction:

- Poll while running tasks exist.
- Manual refresh.
- Retry actions are outside the MVP scope unless an existing backend endpoint already supports them.

### Templates

Purpose: save and reuse generation configurations.

Content:

- Template list and active templates.
- Create/edit form.
- Template detail with config preview.
- Generate from template action.

Interaction:

- Validate template name and config shape.
- From Builder, allow "Save as Template" once a valid request exists.

## UI/UX Design System

Design direction: Dark Futuristic Developer Console.

Principles:

- High information density without visual clutter.
- Strong contrast and readable text before decorative effects.
- Motion should communicate state changes, not distract.
- All interactions must be keyboard accessible.
- Prefer SVG icons over emoji.

Core colors:

```text
Background base: #050816
Background panel: #0F172A
Panel elevated: #111827
Border subtle: rgba(148, 163, 184, 0.18)
Primary neon cyan: #22D3EE
Accent violet: #8B5CF6
Run/success green: #22C55E
Warning amber: #F59E0B
Error rose: #FB7185
Text primary: #F8FAFC
Text secondary: #CBD5E1
Text muted: #64748B
```

Typography:

- Body: system sans or a readable modern sans.
- Code, generator params, and field examples: Fira Code.
- Headings can use a sharper display style, but not at the expense of Chinese and English readability.

Surface style:

- Use dark glass panels with subtle borders.
- Use restrained glow only on active states, focus states, and primary calls to action.
- Avoid overused purple-on-white AI gradients.
- Use layered radial gradients, grid lines, and faint noise texture for atmosphere.

Motion:

- 150-300ms for hover and focus transitions.
- Skeleton loaders for async data.
- Respect `prefers-reduced-motion`.
- Use transform and opacity for performance.

Accessibility:

- Text contrast should meet WCAG AA.
- Touch targets at least 44x44px.
- Visible focus rings.
- Table data must remain readable without relying on color alone.
- Generator Catalog search and result navigation should support keyboard use.

## Routing

Routes:

```text
/login
/dashboard
/builder
/catalog
/tasks
/templates
```

Default route:

- Authenticated users go to `/dashboard`.
- Unauthenticated users go to `/login`.

## State Model

Server state:

- Current user/session validation.
- Templates.
- Recent tasks and task detail.
- Generation submission result.
- Generator catalog metadata if exposed by backend or maintained as frontend seed data.

Client state:

- Auth token and dev mock status.
- Sidebar collapsed state.
- Builder draft.
- Catalog filters and selected generator.
- Theme is dark by default; light mode is not required in MVP.

## Testing Strategy

Unit and component tests:

- Auth store and token handling.
- API client error normalization.
- Builder validation schemas.
- Generator Catalog filtering.
- Template form validation.

Integration tests:

- Login success and failure.
- Builder submits sync generation.
- Async generation returns task id and task page shows running/completed status.
- Template create, edit, delete, and generate-from-template flow.

E2E smoke tests:

- Mock login to dashboard.
- Search generator catalog and add generator to builder.
- Submit an async generation in mock API mode.

## Implementation Phases

Phase 1: Scaffold and foundation.

- Replace incomplete Vue remnants with React + Vite + TypeScript.
- Configure Tailwind CSS, shadcn/ui, aliases, routing, providers, lint/test tooling.
- Build app shell, dark theme tokens, error/loading primitives.

Phase 2: Auth and API client.

- Implement real JWT login and dev mock mode.
- Add API client, response normalization, token injection, and route guards.

Phase 3: Core console pages.

- Build Dashboard, Tasks, and Templates using real backend APIs.
- Add polling and error states.

Phase 4: Generator Builder and Catalog.

- Build field editor, request preview, validation, sync/async submission.
- Build searchable Generator Catalog and add-to-builder flow.

Phase 5: Polish and verification.

- Accessibility pass, responsive states, reduced motion, visual polish.
- Unit/component tests and E2E smoke tests.

## MVP Decisions

The following decisions are fixed for MVP to keep implementation focused:

- Generator metadata starts as frontend seed data under `features/generator-catalog`, derived from documented generator names and examples. The Catalog UI contract should allow a future backend metadata endpoint without changing page behavior.
- Dashboard charts use Recharts because it is lighter and sufficient for KPI and trend visuals in this console.
- Generated output files are shown as backend `outputPath` references in MVP. Direct download is deferred until the backend exposes a safe download endpoint.

## Acceptance Criteria

- A developer can log in or use dev mock mode.
- A developer can browse generator types in Generator Catalog.
- A developer can add generator types to Builder and submit a valid generation request.
- A developer can create and reuse templates.
- A developer can monitor async generation tasks.
- The console presents a cohesive dark futuristic UI with accessible contrast and keyboard-friendly controls.
- The frontend can run locally against Spring Boot on `localhost:8080`.
