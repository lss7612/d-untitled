const API_BASE = '/api/v1/auth/email'

function authHeaders() {
  const token = localStorage.getItem('accessToken')
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  }
}

export async function sendVerifyCode(): Promise<void> {
  const res = await fetch(`${API_BASE}/send-code`, {
    method: 'POST',
    headers: authHeaders(),
  })
  if (!res.ok) {
    const body = await res.json()
    throw new Error(body.message ?? '인증 코드 발송에 실패했습니다.')
  }
}

export interface VerifyCodeResult {
  token: string
}

export async function verifyCode(code: string): Promise<VerifyCodeResult> {
  const res = await fetch(`${API_BASE}/verify`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ code }),
  })
  const body = await res.json()
  if (!res.ok) {
    throw new Error(body.message ?? '인증에 실패했습니다.')
  }
  return body.data
}
