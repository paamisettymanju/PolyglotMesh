import { afterEach, describe, expect, it, vi } from 'vitest'
import { executionEndpoint } from './api'

describe('execution API URL', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('uses the same-origin API path when no base URL is configured', () => {
    vi.stubEnv('VITE_API_BASE_URL', '')

    expect(executionEndpoint()).toBe('/api/execute')
  })

  it('normalizes a configured API base URL', () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.test///')

    expect(executionEndpoint()).toBe('https://api.example.test/api/execute')
  })
})
