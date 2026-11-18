import { useRef, useState } from 'react'
import Editor from '@monaco-editor/react'
import { executionEndpoint } from './api'
import './App.css'

type Language = 'python' | 'javascript'

type ExecutionResult = {
  language: Language
  success: boolean
  output: string
  error: string | null
  executionTimeMs: number
}

function isExecutionResult(value: unknown): value is ExecutionResult {
  return (
    typeof value === 'object' &&
    value !== null &&
    'language' in value &&
    (value.language === 'python' || value.language === 'javascript') &&
    'success' in value &&
    typeof value.success === 'boolean' &&
    'output' in value &&
    typeof value.output === 'string' &&
    'error' in value &&
    (typeof value.error === 'string' || value.error === null) &&
    'executionTimeMs' in value &&
    typeof value.executionTimeMs === 'number' &&
    Number.isFinite(value.executionTimeMs) &&
    value.executionTimeMs >= 0
  )
}

const starterCode: Record<Language, string> = {
  python: 'print("Hello from Python!")\n',
  javascript: 'console.log("Hello from JavaScript!");\n',
}

function App() {
  const [language, setLanguage] = useState<Language>('python')
  const [sourceByLanguage, setSourceByLanguage] =
    useState<Record<Language, string>>(starterCode)
  const [result, setResult] = useState<ExecutionResult | null>(null)
  const [requestError, setRequestError] = useState<string | null>(null)
  const [isRunning, setIsRunning] = useState(false)
  const executionPending = useRef(false)
  const code = sourceByLanguage[language]

  async function runCode() {
    if (executionPending.current) {
      return
    }

    executionPending.current = true
    setIsRunning(true)
    setResult(null)
    setRequestError(null)

    try {
      const response = await fetch(executionEndpoint(), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ language, code }),
      })

      const body: unknown = await response.json()

      if (!response.ok) {
        const message =
          typeof body === 'object' &&
          body !== null &&
          'error' in body &&
          typeof body.error === 'string'
            ? body.error
            : `The execution request failed (HTTP ${response.status}).`
        setRequestError(message)
        return
      }

      if (!isExecutionResult(body)) {
        setRequestError('The execution service returned an invalid response.')
        return
      }

      setResult(body)
    } catch {
      setRequestError(
        'Could not connect to PolyglotMesh. Start the backend and try again.',
      )
    } finally {
      executionPending.current = false
      setIsRunning(false)
    }
  }

  function changeLanguage(nextLanguage: Language) {
    setLanguage(nextLanguage)
    setResult(null)
    setRequestError(null)
  }

  function changeCode(value: string | undefined) {
    setSourceByLanguage((current) => ({ ...current, [language]: value ?? '' }))
  }

  const status = isRunning
    ? 'Running'
    : result?.success
      ? 'Completed'
      : result
        ? 'Execution error'
        : requestError
          ? 'Request error'
          : 'Ready'

  return (
    <main
      className="app-shell"
      onKeyDown={(event) => {
        if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
          event.preventDefault()
          void runCode()
        }
      }}
    >
      <header className="topbar">
        <a className="brand" href="/" aria-label="PolyglotMesh home">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="none">
              <path d="M5 10.5 16 4l11 6.5v11L16 28 5 21.5v-11Z" />
              <path d="m5.5 10.75 10.5 6 10.5-6M16 17v10" />
              <circle cx="16" cy="10" r="2" />
            </svg>
          </span>
          <span className="brand-name">polyglot<span>mesh</span></span>
        </a>
        <span className="runtime-badge">
          <span className="status-dot" aria-hidden="true" />
          Local sandbox
        </span>
      </header>

      <section className="workspace">
        <div className="workspace-heading">
          <div>
            <p className="eyebrow">PLAYGROUND</p>
            <h1>Code. Run. Explore.</h1>
            <p className="subtitle">
              Try Python and JavaScript in your local polyglot runtime.
            </p>
          </div>
          <button
            className="run-button"
            type="button"
            onClick={runCode}
            disabled={isRunning}
          >
            {isRunning ? (
              <span className="spinner" aria-hidden="true" />
            ) : (
              <svg viewBox="0 0 16 16" aria-hidden="true">
                <path d="M5 3.5a.75.75 0 0 1 1.14-.64l6.03 3.75a1.63 1.63 0 0 1 0 2.78l-6.03 3.75A.75.75 0 0 1 5 12.5v-9Z" />
              </svg>
            )}
            {isRunning ? 'Running' : 'Run code'}
            {!isRunning && <kbd>Ctrl ↵</kbd>}
          </button>
        </div>

        <div className="workspace-frame">
          <div className="editor-toolbar">
            <div className="file-tab">
              <span
                className={`language-dot ${language}`}
                aria-hidden="true"
              />
              <span>main.{language === 'python' ? 'py' : 'js'}</span>
              <span className="unsaved-dot" title="Unsaved changes" />
            </div>
            <label className="language-control">
              <span className="sr-only">Programming language</span>
              <select
                value={language}
                onChange={(event) =>
                  changeLanguage(event.target.value as Language)
                }
              >
                <option value="python">Python</option>
                <option value="javascript">JavaScript</option>
              </select>
              <svg viewBox="0 0 16 16" aria-hidden="true">
                <path d="m4 6 4 4 4-4" />
              </svg>
            </label>
          </div>

          <div className="editor-container">
            <Editor
              aria-label={`${language === 'python' ? 'Python' : 'JavaScript'} code editor`}
              height="100%"
              language={language}
              path={`main.${language === 'python' ? 'py' : 'js'}`}
              theme="vs-dark"
              value={code}
              onChange={changeCode}
              options={{
                accessibilitySupport: 'on',
                automaticLayout: true,
                fontFamily: "'Cascadia Code', 'SFMono-Regular', Consolas, monospace",
                fontSize: 14,
                lineHeight: 24,
                minimap: { enabled: false },
                padding: { top: 20, bottom: 20 },
                scrollBeyondLastLine: false,
                tabSize: 4,
                wordWrap: 'on',
              }}
            />
          </div>

          <section className="console-panel" aria-label="Execution console">
            <div className="console-toolbar">
              <h2>Console</h2>
              <div className="console-meta">
                {result && (
                  <span>{Math.max(0, result.executionTimeMs)} ms</span>
                )}
                <span
                  className={`execution-status ${isRunning ? 'active' : ''}`}
                  role="status"
                  aria-live="polite"
                >
                  <span className="status-dot" aria-hidden="true" />
                  {status}
                </span>
              </div>
            </div>
            <pre
              className={`console-output ${requestError || (result && !result.success) ? 'error-output' : ''}`}
              aria-live="polite"
            >
              {isRunning ? (
                <span className="console-placeholder">Running your code…</span>
              ) : requestError ? (
                requestError
              ) : result ? (
                <>
                  {result.output && (
                    <span className="output-text">{result.output}</span>
                  )}
                  {result.error && (
                    <span className="output-error">{result.error}</span>
                  )}
                  {!result.output && !result.error && (
                    <span className="console-placeholder">
                      Program finished without output.
                    </span>
                  )}
                </>
              ) : (
                <span className="console-placeholder">
                  Run your code to see the output here.
                </span>
              )}
            </pre>
          </section>
        </div>

        <footer className="workspace-footer">
          <span>
            Powered by <strong>GraalVM</strong>
          </span>
          <span>Runs locally on your machine</span>
        </footer>
      </section>
    </main>
  )
}

export default App
