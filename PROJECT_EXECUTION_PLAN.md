# PolyglotMesh: nine-day implementation plan

**Start:** Monday, November 16, 2026  
**Finish:** Tuesday, November 24, 2026  
**Goal:** Deliver a working local-first PolyglotMesh application with a Spring Boot WebFlux API, GraalVM-backed Python and JavaScript execution, and a React/Monaco interface.

## Scope and working agreements

The supplied project PDF is a collection of three project briefs. This schedule covers **Project 1, PolyglotMesh**, and the Week 1–2 requirements provided in the project notes: Java, Spring Boot/WebFlux, the GraalVM Polyglot SDK, a sandboxed Python/JavaScript execution API, and a React interface with Monaco and a console. AeroSaga and ByteSwarm are separate briefs and are not part of this implementation.

`PROJECT_INSTRUCTIONS.pdf` contains internship collaboration and conduct guidelines, not additional technical requirements. Follow them while working: communicate blockers, keep changes reviewable, and report progress honestly.

Work in one small, reviewable commit per scheduled day. Run that day's checks before committing. Do not amend or rewrite existing history. **Four groundwork commits were already created on November 16 before this schedule was requested** (`abd8f22`, `4a570a3`, `df19181`, `67e0491`); preserve them. Accordingly, the nine commits listed below are the nine planned daily milestones, in addition to those pre-existing commits. The application bootstrap, GraalVM language dependencies, execution service, and initial WebFlux endpoint already exist in that groundwork; verify and complete them rather than rebuilding them.

Use the project root for Git commands. Keep frontend sources in `frontend/` and backend sources in `backend/`. Do not add credentials or commit local environment files. A feature is done only when it has its documented tests and a user can follow the README to run it.

## Daily milestones and commits

### Day 1 — Monday, November 16 — Stabilize the backend foundation

**Commit:** `Complete backend validation and configuration`

- Review the existing Spring Boot/WebFlux bootstrap, `/api/execute` controller, request/response records, and GraalVM execution service.
- Complete and verify the current in-progress request validation and consistent HTTP 400 error responses.
- Add application defaults for the HTTP port and explicitly configured frontend CORS origins; do not allow every origin by default.
- Add a health endpoint so a local run can be checked without executing user code.
- Add the Maven Wrapper if one is not already present, so the build does not depend on Maven being installed globally.

**Acceptance checks**

- `.\backend\mvnw.cmd -f .\backend\pom.xml test` passes.
- The application starts and its health endpoint responds successfully.
- Invalid JSON, missing fields, unsupported languages, and code over the documented source-size limit produce a clear 400 response, not a 500.
- Check `git status` and commit only the intended backend changes.

### Day 2 — Tuesday, November 17 — Verify polyglot execution and isolation

**Commit:** `Test sandboxed Python and JavaScript execution`

- Add focused backend tests for Python and JavaScript output, empty output, syntax/runtime errors, supported language aliases, and execution-time reporting.
- Test that each execution uses a fresh context and does not expose host classes, filesystem access, environment variables, native access, or thread creation.
- Verify output is bounded and resource-limit failures produce a failed execution result without leaking a Java stack trace or internal host details to the client.
- Ensure GraalVM language runtimes actually resolve and execute on the supported local JDK; document any GraalVM/JDK compatibility requirement discovered.

**Acceptance checks**

- `.\backend\mvnw.cmd -f .\backend\pom.xml test` passes, including both language smoke tests.
- Direct API smoke tests return captured output for Python `print("Hello from Python")` and JavaScript `console.log("Hello from JavaScript")`.
- Attempts to access host Java classes or local files fail without exposing their contents.

### Day 3 — Wednesday, November 18 — Create the React frontend

**Commit:** `Scaffold React TypeScript execution workspace`

- Create the `frontend/` React and TypeScript application using Vite.
- Add Monaco Editor and define the Python and JavaScript editor modes and starter examples.
- Add a single documented frontend development command and configurable API base URL.
- Add linting and frontend test scripts; commit the package manifest and lockfile, not `node_modules/`.

**Acceptance checks**

- `npm.cmd --prefix frontend ci` installs from the committed lockfile.
- `npm.cmd --prefix frontend run build` succeeds.
- `npm.cmd --prefix frontend run lint` succeeds.
- `npm.cmd --prefix frontend run test` succeeds.

### Day 4 — Thursday, November 19 — Build the editor and console interface

**Commit:** `Build Monaco editor and execution console`

- Implement the PolyglotMesh workspace with a language selector, Monaco editor, Run button, and console/output panel.
- Make the initial layout usable at desktop and narrow viewport widths; ensure the editor and output area can be used without layout overflow.
- Show the selected language clearly and use appropriate starter code for Python and JavaScript.
- Display execution state and distinguish normal output from errors.

**Acceptance checks**

- The frontend production build, lint, and tests pass.
- The editor changes language when the user changes the selector.
- The Run button cannot start duplicate executions while a request is pending.
- Empty output, multiline output, and execution errors are distinguishable in the console.

### Day 5 — Friday, November 20 — Connect the interface to the execution API

**Commit:** `Connect frontend to execution API`

- Send the selected language and editor source as JSON to `POST /api/execute`.
- Render the backend's output, error, success status, and execution time without inventing success when a request fails.
- Handle loading, backend-unavailable, malformed-response, HTTP 400, and execution-error states accessibly.
- Confirm local CORS configuration allows the configured development origin and rejects unconfigured origins.

**Acceptance checks**

- Start the backend and frontend using the documented commands; execute one Python and one JavaScript example from the UI.
- Confirm a deliberate runtime error is presented as an execution error, and stopping the backend produces a clear connection error.
- Frontend tests cover success and failed HTTP responses; all Day 3–4 checks still pass.

### Day 6 — Saturday, November 21 — Harden execution limits and API behavior

**Commit:** `Bound execution inputs output and workload`

- Review source-size, output-size, and GraalVM statement limits; make limits explicit, configurable where appropriate, and documented.
- Add tests for oversized source, truncated/excessive output, statement/resource exhaustion, and malformed requests.
- Apply bounded request sizes, execution concurrency, and backpressure so simultaneous requests cannot grow without limit.
- Ensure unexpected server-side exceptions are logged server-side and return a generic, correctly shaped error to the client.
- Document the trust boundary: in-process GraalVM restrictions are defense in depth, not a substitute for OS/container isolation against hostile code. Do not describe the local development server as safe for unrestricted public multi-tenant use.

**Acceptance checks**

- Backend tests verify configured input/output/resource limits and invalid requests.
- A deliberately resource-intensive program fails within documented limits and leaves the application responsive to a health check.
- No request can disable host-access restrictions or choose an arbitrary language/runtime.

### Day 7 — Sunday, November 22 — Add end-to-end coverage and interface accessibility

**Commit:** `Test and polish end-to-end execution flow`

- Add integration coverage for the Spring endpoint, JSON contract, and error status codes.
- Add frontend tests for language selection, editing, pending execution, successful output, runtime errors, and API failures.
- Improve keyboard operation, visible focus, accessible names/live status announcements, and readable console output.
- Fix only issues exposed by these tests or necessary accessibility checks.

**Acceptance checks**

- Backend unit and integration tests and all frontend tests pass.
- The Run workflow works with keyboard controls as well as a mouse.
- No secrets or raw stack traces appear in client responses.

### Day 8 — Monday, November 23 — Document setup and repeatable local operation

**Commit:** `Document PolyglotMesh setup and operation`

- Write a concise root README with prerequisites, exact backend/frontend startup commands, health check, API request/response example, supported languages, limits, and troubleshooting.
- Document which JDK/GraalVM combination was actually verified; distinguish the Java JDK used to run Spring Boot from the GraalVM language runtime dependencies.
- Add safe example configuration files if needed; ignore real local environment files and never add secrets.
- Add Dockerfiles or Compose only if they can be built and verified on this repository's actual target environment; do not make an untested container file a release requirement.

**Acceptance checks**

- Follow the README on a clean checkout using only its documented steps.
- Verify the documented API example against the running backend.
- `git status` contains no generated binaries, dependency directories, extracted PDF/OCR files, or secret-bearing configuration.

### Day 9 — Tuesday, November 24 — Final acceptance and release-ready commit

**Commit:** `Complete PolyglotMesh local execution experience`

- Run the complete backend tests, frontend tests, lint, and production build.
- Re-test the full documented flow for Python and JavaScript, invalid requests, execution errors, and backend-unavailable states.
- Fix remaining defects in the required flow; update documentation when behavior changes.
- Confirm the working tree is clean apart from the two user-provided, untracked project PDFs; never add those PDFs, rewrite prior commits, or push without authorization.
- Record any unverified production-hardening work as a limitation instead of claiming completion.

**Acceptance checks**

- `.\backend\mvnw.cmd -f .\backend\pom.xml test` passes.
- `npm.cmd --prefix frontend ci`, `npm.cmd --prefix frontend run lint`, `npm.cmd --prefix frontend run test`, and `npm.cmd --prefix frontend run build` pass.
- A fresh local start and the README's Python and JavaScript examples both work end to end.
- Review `git diff --check`, the final commit, and `git status --short`.

## Final deliverable

A documented, locally runnable PolyglotMesh application: a Spring Boot WebFlux API executes Python and JavaScript using GraalVM with restricted host access and explicit limits; a React/Monaco UI sends code to that API and clearly presents output, errors, and execution state; and automated checks cover the supported flow and its failure cases.

**Out of scope for this nine-day plan:** AeroSaga drone workflows, ByteSwarm distributed compute, a public multi-tenant service, authentication/account management, billing, and claims that in-process context restrictions alone provide a complete hostile-code security boundary.
