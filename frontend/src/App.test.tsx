import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

vi.mock('@monaco-editor/react', () => ({
  default: ({
    language,
    value,
    onChange,
  }: {
    language: string
    value: string
    onChange: (value: string) => void
  }) => (
    <textarea
      aria-label={`${language} code editor`}
      value={value}
      onChange={(event) => onChange(event.target.value)}
    />
  ),
}))

describe('PolyglotMesh workspace', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('starts in Python and submits code to the execution API', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          language: 'python',
          success: true,
          output: 'Hello from Python!\n',
          error: null,
          executionTimeMs: 8,
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
    )
    render(<App />)

    expect(screen.getByLabelText('Programming language')).toHaveValue('python')
    await userEvent.click(screen.getByRole('button', { name: /run code/i }))

    expect(await screen.findByText('Hello from Python!')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Completed')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/execute',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          language: 'python',
          code: 'print("Hello from Python!")\n',
        }),
      }),
    )
  })

  it('switches language and preserves each language editor contents', async () => {
    render(<App />)

    fireEvent.change(screen.getByLabelText('Programming language'), {
      target: { value: 'javascript' },
    })
    const editor = screen.getByLabelText('javascript code editor')
    expect(editor).toHaveValue('console.log("Hello from JavaScript!");\n')

    await userEvent.clear(editor)
    await userEvent.type(editor, 'console.log("edited")')

    fireEvent.change(screen.getByLabelText('Programming language'), {
      target: { value: 'python' },
    })
    fireEvent.change(screen.getByLabelText('Programming language'), {
      target: { value: 'javascript' },
    })

    expect(screen.getByLabelText('javascript code editor')).toHaveValue(
      'console.log("edited")',
    )
  })

  it('submits the JavaScript starter program with JavaScript syntax selected', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          language: 'javascript',
          success: true,
          output: 'Hello from JavaScript!\n',
          error: null,
          executionTimeMs: 2,
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
    )
    render(<App />)

    fireEvent.change(screen.getByLabelText('Programming language'), {
      target: { value: 'javascript' },
    })
    expect(screen.getByLabelText('javascript code editor')).toHaveValue(
      'console.log("Hello from JavaScript!");\n',
    )
    await userEvent.click(screen.getByRole('button', { name: /run code/i }))

    expect(await screen.findByText('Hello from JavaScript!')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/execute',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          language: 'javascript',
          code: 'console.log("Hello from JavaScript!");\n',
        }),
      }),
    )
  })

  it('renders execution errors without presenting them as successful output', async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response(
        JSON.stringify({
          language: 'python',
          success: false,
          output: '',
          error: 'Execution exceeded the resource limit.',
          executionTimeMs: 4,
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
    )
    render(<App />)

    await userEvent.click(screen.getByRole('button', { name: /run code/i }))

    expect(
      await screen.findByText('Execution exceeded the resource limit.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Execution error')
  })

  it('displays API validation errors and handles unavailable backends', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ error: 'code is required.' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    render(<App />)

    await userEvent.click(screen.getByRole('button', { name: /run code/i }))
    expect(await screen.findByText('code is required.')).toBeInTheDocument()

    fetchMock.mockRejectedValue(new Error('Backend is unavailable'))
    await userEvent.click(screen.getByRole('button', { name: /run code/i }))
    await waitFor(() => {
      expect(
        screen.getByText(
          'Could not connect to PolyglotMesh. Start the backend and try again.',
        ),
      ).toBeInTheDocument()
    })
  })

  it('announces pending execution and distinguishes successful empty output', async () => {
    let resolveResponse: ((response: Response) => void) | undefined
    vi.mocked(fetch).mockReturnValue(
      new Promise((resolve) => {
        resolveResponse = resolve
      }),
    )
    render(<App />)

    await userEvent.click(screen.getByRole('button', { name: /run code/i }))
    expect(screen.getByRole('status')).toHaveTextContent('Running')

    resolveResponse?.(
      new Response(
        JSON.stringify({
          language: 'python',
          success: true,
          output: '',
          error: null,
          executionTimeMs: 1,
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
    )

    expect(
      await screen.findByText('Program finished without output.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Completed')
  })

  it('rejects malformed success responses and prevents duplicate submissions', async () => {
    const fetchMock = vi.mocked(fetch)
    let resolveResponse: ((response: Response) => void) | undefined
    fetchMock.mockReturnValue(
      new Promise((resolve) => {
        resolveResponse = resolve
      }),
    )
    render(<App />)

    await userEvent.click(screen.getByRole('button', { name: /run code/i }))
    const runButton = screen.getByRole('button', { name: /running/i })
    expect(runButton).toBeDisabled()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    fireEvent.keyDown(screen.getByRole('main'), {
      key: 'Enter',
      ctrlKey: true,
    })
    expect(fetchMock).toHaveBeenCalledTimes(1)

    resolveResponse?.(
      new Response(JSON.stringify({ success: true }), {
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    expect(
      await screen.findByText(
        'The execution service returned an invalid response.',
      ),
    ).toBeInTheDocument()
  })
})
