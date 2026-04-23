import { apiFetch } from './client'

export interface AppConfigResponse {
  type: string
  contents: string
  updatedBy: number | null
  updatedAt: string | null
}

export function getConfig(type: string): Promise<AppConfigResponse> {
  return apiFetch<AppConfigResponse>(`/api/v1/developer/config/${type}`)
}

export function updateConfig(type: string, emails: string[]): Promise<AppConfigResponse> {
  return apiFetch<AppConfigResponse>(`/api/v1/developer/config/${type}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ contents: JSON.stringify(emails) }),
  })
}
