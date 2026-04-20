/**
 * 모든 백엔드 API 호출은 이 함수를 거친다.
 * - 자동으로 Authorization 헤더 추가
 * - 401 응답 시 localStorage 클리어 + /login 리다이렉트
 * - { success, data, message } 래퍼 언래핑하여 data만 반환
 */
export async function apiFetch<T = unknown>(
  path: string,
  init: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('accessToken')
  const headers: HeadersInit = {
    ...(init.body ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(init.headers ?? {}),
  }

  const res = await fetch(path, { ...init, headers })

  if (res.status === 401) {
    localStorage.removeItem('accessToken')
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    throw new Error('인증이 필요합니다.')
  }

  let body: { success: boolean; data: T; message?: string } | null = null
  try {
    body = await res.json()
  } catch {
    if (!res.ok) throw new Error(`서버 오류 (${res.status})`)
    throw new Error('응답 형식 오류')
  }

  if (!res.ok || (body && body.success === false)) {
    throw new Error(body?.message ?? `요청 실패 (${res.status})`)
  }

  return body!.data
}
