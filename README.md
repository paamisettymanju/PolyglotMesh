# PolyglotMesh

PolyglotMesh is a local-first code playground for running Python and JavaScript with Spring Boot, GraalVM, React, and Monaco Editor. The application consists of a Spring WebFlux execution API in `backend/` and a React/TypeScript workspace in `frontend/`.

## Requirements

- Java 17 or later
- Node.js 20.19+ or 22.12+ and npm (Vite 8 requirement)
- Network access on the first run to download Maven and npm dependencies

The backend uses Maven Wrapper; a separate Maven installation is not required.
The runtime verified for this milestone is Microsoft OpenJDK 21.0.13 with
GraalVM Polyglot and the JavaScript/Python language dependencies at 24.1.2.
The application uses a standard JDK; a GraalVM JDK distribution is not required.

## Run locally

Run the following commands from the repository root in PowerShell. Confirm the
installed runtimes first:

```powershell
java -version
node --version
npm.cmd --version
```

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
Use **Ctrl+Enter** (or **Command+Enter** on macOS) to run the selected
language's code from the keyboard.

The Vite development server proxies `/api` and `/actuator` to
`http://localhost:8080` by default. To call a backend directly, set
`VITE_API_BASE_URL` before starting or building the frontend; for example:

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm.cmd --prefix frontend run dev
```

For a browser request to a different backend origin, add the frontend origin
(scheme, host, and port) to `APP_CORS_ALLOWED_ORIGINS` on the backend, for
example `https://playground.example`. `VITE_API_BASE_URL` is embedded when Vite
starts/builds, so configure it before starting the development server or
building the frontend. The backend's allowed origins default to
`http://localhost:5173`; `http://127.0.0.1:5173` is a different origin and must
be explicitly allowed when using it to access the backend directly.

## Backend configuration

| Variable | Default | Description |
| --- | --- | --- |
| `PORT` | `8080` | Spring Boot HTTP port |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated list of explicitly permitted browser origins |
| `APP_EXECUTION_MAX_CONCURRENT` | `4` | Maximum number of guest programs executing at the same time; excess runs receive HTTP 429 |
| `VITE_API_BASE_URL` | Empty | Optional frontend API base URL; the Vite development proxy is used when unset |

The health endpoint is `GET http://localhost:8080/actuator/health`.
The supported language list is available at `GET http://localhost:8080/api/languages`.

Verify the backend from PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/languages
```

Submit code with `POST http://localhost:8080/api/execute`:

```json
{
  "language": "python",
  "code": "print(\"Hello from PolyglotMesh!\")"
}
```

For a repeatable PowerShell smoke test, the following should return
`success: True` and `Hello from PolyglotMesh!` followed by a newline:

```powershell
$body = @{
  language = "python"
  code = 'print("Hello from PolyglotMesh!")'
} | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/execute `
  -Method Post -ContentType "application/json" -Body $body
```

For JavaScript, the corresponding request is:

```powershell
$body = @{
  language = "javascript"
  code = 'console.log("Hello from JavaScript!")'
} | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/execute `
  -Method Post -ContentType "application/json" -Body $body
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
limited to 50,000 characters and each JSON request body to 128 KB. Oversized
source returns HTTP 400 and oversized request bodies return HTTP 413, with a
JSON `error` message. Invalid JSON and unsupported languages also return HTTP
400. When all execution slots are occupied, the API rejects new runs with HTTP
429 rather than queueing guest work.
Unexpected server failures return HTTP 500 with a generic JSON error; internal
exception details are logged on the backend, not exposed to clients.
The UI reports the execution duration only when the API response includes it;
invalid response bodies and HTTP failures are shown as request errors, not
successful executions. A direct connection failure or HTTP 502/503/504 response
from a development proxy is shown as a backend-unavailable message.

Guest-code failures (for example, syntax errors or resource exhaustion) are
execution results with HTTP 200 and `success: false`. Invalid request bodies
return HTTP 400, bodies over 128 KB return HTTP 413, full execution capacity
returns HTTP 429, and unexpected server errors return HTTP 500.

## Troubleshooting

- **Backend does not start:** confirm Java 17+ is on `PATH`, allow the Maven
  Wrapper to download Maven/dependencies on first use, and check whether port
  8080 is already occupied. Set `PORT` to a free port if needed; update the Vite
  proxy target in `frontend/vite.config.ts` to match that port.
- **Frontend dependencies or Vite fail to install/start:** confirm Node.js is
  20.19+ or 22.12+, then run `npm.cmd --prefix frontend ci` from the repository
  root. The first install requires network access.
- **UI reports that the backend is unavailable:** keep the backend running,
  verify `http://localhost:8080/actuator/health`, and confirm the Vite proxy
  targets the backend port.
- **Direct browser requests fail with a CORS error:** ensure the exact frontend
  origin (including scheme, host, and port) is present in
  `APP_CORS_ALLOWED_ORIGINS`, then restart the backend. Requests through the
  same-origin Vite proxy do not require changing CORS configuration.
- **Execution is rejected:** review the request-size and source-code limits
  above; HTTP 429 means all configured guest-execution slots are occupied, so
  retry after an active run completes.

## Checks

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml test
npm.cmd --prefix frontend run test
npm.cmd --prefix frontend run lint
npm.cmd --prefix frontend run build
```

## Execution security

The backend disables GraalVM host-class lookup, host access, host file and network I/O, native access, environment access, thread creation, and guest polyglot access. It also bounds source size, captured output, and guest statements. These in-process restrictions are defense in depth and do **not** make executing untrusted code in a public multi-tenant service safe; production deployments that execute hostile code require additional operating-system or container isolation.

Each request is limited to 128 KB, 50,000 source characters, 100,000 guest
statements, and 64,000 captured bytes per output and error stream. At most four
guest programs execute concurrently by default; configure this with
`APP_EXECUTION_MAX_CONCURRENT`. Exceeding an execution resource or output limit
returns a failed execution result rather than successful output; excess
concurrent requests receive HTTP 429. In-process limits are not a hard wall-clock
timeout or a substitute for OS/container isolation.
