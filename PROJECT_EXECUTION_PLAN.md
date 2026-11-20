# PolyglotMesh: nine-day implementation plan

**Start:** Monday, November 16, 2026  
**Finish:** Tuesday, November 24, 2026  
**Goal:** Build and verify the backend and user interface together every day, delivering a working PolyglotMesh application with a Spring Boot WebFlux API, GraalVM-backed Python and JavaScript execution, and a React/Monaco interface.

## Required technologies

- Java
- Apache Maven
- Spring Boot
- GraalVM
- React
- Monaco Editor
- Git and GitHub

## Scope and working agreements

The supplied project PDF is a collection of three project briefs. This schedule covers **Project 1, PolyglotMesh**, and the Week 1–2 requirements provided in the project notes: Java, Spring Boot/WebFlux, the GraalVM Polyglot SDK, a sandboxed Python/JavaScript execution API, and a React interface with Monaco and a console. AeroSaga and ByteSwarm are separate briefs and are not part of this implementation.

`PROJECT_INSTRUCTIONS.pdf` contains internship collaboration and conduct guidelines, not additional technical requirements. Follow them while working: communicate blockers, keep changes reviewable, and report progress honestly.

Work on both the backend and the UI on every scheduled day. Each daily milestone includes coordinated backend, frontend, and integration work; do not defer all UI work to a later milestone or make the plan backend-only. Keep each day's changes cohesive, run that day's backend and frontend checks, and commit and push the verified daily changes to GitHub. Include the plan/README Markdown updates that belong to those daily deliverables. Use a concise, business-appropriate commit message that describes the capability delivered, not a generic day number. Do not amend or rewrite existing history. **Four groundwork commits were already created on November 16 before this schedule was requested** (`abd8f22`, `4a570a3`, `df19181`, `67e0491`); preserve them. The application bootstrap, GraalVM language dependencies, execution service, and initial WebFlux endpoint already exist in that groundwork; verify and complete them rather than rebuilding them.

Use the project root for Git commands. Keep frontend sources in `frontend/` and backend sources in `backend/`. Update this plan as scope or daily progress changes; include relevant Markdown documentation in the day's verified commit and push. Do not add credentials or generated files to commits. Run the relevant backend tests, frontend tests, lint, and production build before pushing the changes applicable to that milestone. A feature is done only when it has its documented tests and a user can follow the README to run it.

## Daily implementation milestones

**Progress update — Tuesday, November 17, 2026:** Day 1 is complete. Day 2 is
implemented and its backend/frontend checks plus browser smoke tests are passing.
The verified runtime is Microsoft OpenJDK 21.0.13 with GraalVM Polyglot 24.1.2.

**Progress update — Wednesday, November 18, 2026:** Day 2 has no pending tasks.
Day 3 is implemented: API request/response and error contracts are covered,
frontend API URL configuration and both language starter workflows are tested,
and Python, JavaScript, and runtime-error requests were verified through the
browser against the separately configured backend.

**Progress update — Thursday, November 19, 2026:** Day 3 has no pending tasks.
Day 4 is implemented: the editor and console use a responsive split layout;
multiline/empty output, pending requests, duplicate-run prevention, runtime and
HTTP failures are covered. Browser checks verified desktop/mobile layouts,
both language runs, and the disconnected-backend message.

**Progress update — Friday, November 20, 2026:** Day 4 has no pending tasks.
Day 5 is implemented: stable execution error and response contracts, explicit
CORS assertions, accessible pending state, optional timing metadata, malformed
and failed HTTP response handling, and direct API-base configuration are
covered. Browser checks confirmed both language runs, runtime errors, and
configured/unconfigured CORS origins.

### Day 1 — Monday, November 16 — Deliver the full-stack application foundation

- **Backend:** verify the Spring Boot/WebFlux bootstrap, `/api/execute` controller, and GraalVM execution service; implement request validation and clear HTTP 400 errors; configure the port and an explicit frontend CORS allowlist; expose health; add the Maven Wrapper if needed.
- **UI:** create the React and TypeScript application, integrate Monaco, and deliver an initial usable workspace with Python/JavaScript selection, a Run button, and a console.
- **Integration:** connect the Run button to `POST /api/execute` and display output, execution errors, and unavailable-backend errors.
- Update this plan and the README with the real Day 1 stack, setup commands, configuration, and current project status.

**Acceptance checks**

- `.\backend\mvnw.cmd -f .\backend\pom.xml test` passes.
- The application starts and its health endpoint responds successfully.
- Invalid JSON, missing fields, unsupported languages, and code over the documented source-size limit produce a clear 400 response, not a 500.
- Frontend tests, lint, and production build pass; a Python and a JavaScript example can be submitted from the interface.
- Commit the tested backend, frontend, and related Markdown changes using a descriptive business message and push the daily changes.

### Day 2 — Tuesday, November 17 — Verify polyglot execution and isolation

- **Backend:** add focused tests for Python and JavaScript output, empty output, syntax/runtime errors, supported language aliases, and execution-time reporting; verify fresh contexts, restricted host access, bounded output, and resource exhaustion behavior.
- **UI:** add execution-pending state and clearly distinguish successful output, runtime errors, and request failures without presenting failures as successful runs.
- **Integration:** run both language examples through the browser UI and verify error and timeout/resource failure responses.
- Document the GraalVM/JDK combination actually verified.

**Acceptance checks**

- `.\backend\mvnw.cmd -f .\backend\pom.xml test` passes, including both language smoke tests.
- Direct API smoke tests return captured output for Python `print("Hello from Python")` and JavaScript `console.log("Hello from JavaScript")`.
- Attempts to access host Java classes or local files fail without exposing their contents in the editor console.
- Run backend and frontend checks; commit and push the complete verified daily changes.

### Day 3 — Wednesday, November 18 — Expand the Monaco editor workspace

- **Backend:** add API contract tests for request and response JSON, health, CORS configuration, and stable errors.
- **UI:** improve the Monaco workspace with correct Python/JavaScript syntax modes, starter examples, and language-specific code.
- **Integration:** configure the frontend API base URL and verify requests against the running backend.
- Keep Vite, TypeScript, lint/test scripts, and dependency lockfiles reproducible; never commit `node_modules/`.

**Acceptance checks**

- `npm.cmd --prefix frontend ci` installs from the committed lockfile.
- `npm.cmd --prefix frontend run build` succeeds.
- `npm.cmd --prefix frontend run lint` succeeds.
- `npm.cmd --prefix frontend run test` succeeds.
- Backend tests pass, and the workspace can submit code in each supported language.
- Commit and push the verified backend, UI, and project setup updates.

### Day 4 — Thursday, November 19 — Build the editor and console interface

- **Backend:** verify the documented execution API response, empty output, validation failures, and server-error behavior.
- **UI:** refine the editor and console split layout for desktop and narrow widths, selection and starter code, execution status, and readable output/error presentation.
- **Integration:** test loading, duplicate-run prevention, multiline output, empty output, runtime errors, and disconnected-backend behavior together.

**Acceptance checks**

- The frontend production build, lint, and tests pass.
- The editor changes language when the user changes the selector.
- The Run button cannot start duplicate executions while a request is pending.
- Empty output, multiline output, and execution errors are distinguishable in the console.
- Backend tests, frontend tests, lint, and the production build pass; commit and push all verified daily changes.

### Day 5 — Friday, November 20 — Connect the interface to the execution API

- **Backend:** stabilize `/api/execute`, health, error contracts, and explicitly configured CORS behavior.
- **UI:** handle loading, backend-unavailable, malformed-response, HTTP 400, and runtime-error states accessibly; report execution time only when supplied by the backend.
- **Integration:** run Python and JavaScript examples in the browser, verify the displayed API results, and confirm configured origins work while unconfigured origins are rejected.

**Acceptance checks**

- Start the backend and frontend using the documented commands; execute one Python and one JavaScript example from the UI.
- Confirm a deliberate runtime error is presented as an execution error, and stopping the backend produces a clear connection error.
- Frontend tests cover success and failed HTTP responses; all Day 3–4 checks still pass.
- Run backend checks, frontend tests, lint, and build; commit and push the full-stack fix.

### Day 6 — Saturday, November 21 — Harden execution limits and API behavior

- **Backend:** make source, output, resource, request, and concurrency limits explicit and test oversized input, output truncation, exhaustion, and unexpected errors.
- **UI:** explain rejected/limited runs clearly and safely; preserve usable editor state and prevent duplicate submissions while a run is pending.
- **Integration:** exercise resource-limit errors end-to-end and confirm the application and health check remain responsive.
- Document the trust boundary: in-process GraalVM restrictions are defense in depth, not a substitute for OS/container isolation against hostile code.

**Acceptance checks**

- Backend tests verify configured input/output/resource limits and invalid requests.
- A deliberately resource-intensive program fails within documented limits and leaves the application responsive to a health check.
- No request can disable host-access restrictions or choose an arbitrary language/runtime.
- UI limit/error states are tested; commit and push the verified full-stack hardening.

### Day 7 — Sunday, November 22 — Add end-to-end coverage and interface accessibility

- **Backend:** extend API/integration coverage for successful execution, the JSON contract, validation, CORS, and error statuses.
- **UI:** test language selection, editing, pending execution, successful output, runtime errors, and API failures; improve keyboard operation, focus, accessible labels, and live status announcements.
- **Integration:** run both languages and error flows with assistive/keyboard controls where applicable.

**Acceptance checks**

- Backend unit and integration tests and all frontend tests pass.
- The Run workflow works with keyboard controls as well as a mouse.
- No secrets or raw stack traces appear in client responses.
- Commit and push all tested backend, UI, and accessibility fixes.

### Day 8 — Monday, November 23 — Document setup and repeatable local operation

- **Backend and UI:** address defects found by following the README's exact startup and Python/JavaScript workflows on a clean checkout.
- Document prerequisites, backend and frontend startup, API examples, health, supported languages, limits, CORS settings, and troubleshooting; distinguish the runtime JDK from GraalVM language dependencies.
- Add safe example configuration only when needed. Add Docker support only if it can be built and tested.
- Verify every interface, configuration, and API example in the documentation against the actual application.

**Acceptance checks**

- Follow the README on a clean checkout using only its documented steps.
- Verify the documented API example against the running backend.
- `git status` contains no generated binaries, dependency directories, extracted PDF/OCR files, or secret-bearing configuration.
- Commit and push the tested implementation and matching Markdown documentation.

### Day 9 — Tuesday, November 24 — Final acceptance and release-ready commit

- Run the complete backend and frontend test suites, frontend lint, and production build.
- Re-test the documented UI-to-API workflows for Python and JavaScript, invalid requests, runtime/resource errors, and backend-unavailable states.
- Fix remaining defects across both UI and backend and update the docs to match the verified behavior.
- Confirm generated files, local secrets, and the user-provided PDFs remain untracked and uncommitted; never rewrite prior commits.
- Record any unverified production-hardening work as a limitation instead of claiming completion.

**Acceptance checks**

- `.\backend\mvnw.cmd -f .\backend\pom.xml test` passes.
- `npm.cmd --prefix frontend ci`, `npm.cmd --prefix frontend run lint`, `npm.cmd --prefix frontend run test`, and `npm.cmd --prefix frontend run build` pass.
- A fresh local start and the README's Python and JavaScript examples both work end to end.
- Review `git diff --check`, the final commit, and `git status --short`.
- Commit and push the tested release-ready backend, UI, and documentation changes.

## Final deliverable

A documented, locally runnable PolyglotMesh application: a Spring Boot WebFlux API executes Python and JavaScript using GraalVM with restricted host access and explicit limits; a React/Monaco UI sends code to that API and clearly presents output, errors, and execution state; and automated checks cover the supported flow and its failure cases.

**Out of scope for this nine-day plan:** AeroSaga drone workflows, ByteSwarm distributed compute, a public multi-tenant service, authentication/account management, billing, and claims that in-process context restrictions alone provide a complete hostile-code security boundary.
