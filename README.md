# PolyglotMesh

PolyglotMesh is a local-first code playground for running Python and JavaScript with Spring Boot, GraalVM, React, and Monaco Editor. The application consists of a Spring WebFlux execution API in `backend/` and a React/TypeScript workspace in `frontend/`.

## Requirements

- Java 17 or later
- Node.js and npm
- Network access on the first run to download Maven and npm dependencies

The backend uses Maven Wrapper; a separate Maven installation is not required.
The runtime verified for this milestone is Microsoft OpenJDK 21.0.13 with
GraalVM Polyglot and the JavaScript/Python language dependencies at 24.1.2.
The application uses a standard JDK; a GraalVM JDK distribution is not required.

## Run locally

Start the backend in one terminal:

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml spring-boot:run
```

In another terminal, install frontend dependencies and start the UI:

```powershell
npm.cmd --prefix frontend ci
npm.cmd --prefix frontend run dev
```

Open the frontend URL printed by Vite (by default, `http://localhost:5173`). The Vite development server proxies API requests to the backend at `http://localhost:8080`.

The UI defaults to the Python editor. Choose JavaScript in the language selector, edit the Monaco editor, and select **Run code** to submit a program and view its output or error.

The Vite development server proxies `/api` and `/actuator` to
`http://localhost:8080` by default. To call a backend directly, set
`VITE_API_BASE_URL` before starting or building the frontend; for example:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm.cmd --prefix frontend run dev
```

For a browser request to a different backend origin, add the frontend origin to
`APP_CORS_ALLOWED_ORIGINS` on the backend. `VITE_API_BASE_URL` is embedded when
Vite starts/builds, so configure it before starting the development server or
building the frontend.

## Backend configuration

| Variable | Default | Description |
| --- | --- | --- |
| `PORT` | `8080` | Spring Boot HTTP port |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated list of explicitly permitted browser origins |
| `VITE_API_BASE_URL` | Empty | Optional frontend API base URL; the Vite development proxy is used when unset |

The health endpoint is `GET http://localhost:8080/actuator/health`.
The supported language list is available at `GET http://localhost:8080/api/languages`.

Submit code with `POST http://localhost:8080/api/execute`:

```json
{
  "language": "python",
  "code": "print(\"Hello from PolyglotMesh!\")"
}
```

The response contains the selected `language`, a `success` flag, captured `output`, an `error` string or `null`, and `executionTimeMs`:

```json
{
  "language": "python",
  "success": true,
  "output": "Hello from PolyglotMesh!\n",
  "error": null,
  "executionTimeMs": 12
}
```

Supported language values are `python`, `javascript`, and `js`. Source code is
limited to 50,000 characters; invalid JSON, unsupported languages, and invalid
or oversized requests return HTTP 400 with a JSON `error` message.
Unexpected server failures return HTTP 500 with a generic JSON error; internal
exception details are logged on the backend, not exposed to clients.
The UI reports the execution duration only when the API response includes it;
invalid response bodies and HTTP failures are shown as request errors, not
successful executions. A direct connection failure or HTTP 502/503/504 response
from a development proxy is shown as a backend-unavailable message.

## Checks

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml test
npm.cmd --prefix frontend run test
npm.cmd --prefix frontend run lint
npm.cmd --prefix frontend run build
```

## Execution security

The backend disables GraalVM host-class lookup, host access, host file and network I/O, native access, environment access, thread creation, and guest polyglot access. It also bounds source size, captured output, and guest statements. These in-process restrictions are defense in depth and do **not** make executing untrusted code in a public multi-tenant service safe; production deployments that execute hostile code require additional operating-system or container isolation.

Each request is limited to 50,000 source characters, 100,000 guest statements,
and 64,000 captured bytes per output stream. Exceeding an execution resource or
output limit returns a failed execution result rather than successful output.
