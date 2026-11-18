export function executionEndpoint(
  baseUrl: string | undefined = import.meta.env.VITE_API_BASE_URL,
): string {
  return `${(baseUrl ?? '').replace(/\/+$/, '')}/api/execute`
}
