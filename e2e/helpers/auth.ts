import { Page } from '@playwright/test'

/**
 * JWT payload를 서명 없이 base64url 인코딩하여 fake JWT 토큰을 구성합니다.
 * 프론트엔드는 서명 검증 없이 payload를 decode하므로 이 방식으로 동작합니다.
 */
function buildFakeJwt(payload: object): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')

  const body = btoa(JSON.stringify(payload))
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')

  const signature = 'fake-signature'

  return `${header}.${body}.${signature}`
}

function nowSeconds(): number {
  return Math.floor(Date.now() / 1000)
}

/**
 * 이미 이메일 인증이 완료된 사용자의 토큰(emailVerified=true)을 localStorage에 주입합니다.
 */
export async function setVerifiedToken(page: Page): Promise<void> {
  const now = nowSeconds()
  const token = buildFakeJwt({
    sub: '1',
    emailVerified: true,
    iat: now,
    exp: now + 3600,
  })
  await page.addInitScript((t) => {
    window.localStorage.setItem('accessToken', t)
  }, token)
}

/**
 * 이메일 인증이 완료되지 않은 사용자의 토큰(emailVerified=false)을 localStorage에 주입합니다.
 */
export async function setUnverifiedToken(page: Page): Promise<void> {
  const now = nowSeconds()
  const token = buildFakeJwt({
    sub: '1',
    emailVerified: false,
    iat: now,
    exp: now + 3600,
  })
  await page.addInitScript((t) => {
    window.localStorage.setItem('accessToken', t)
  }, token)
}

/**
 * localStorage의 accessToken을 초기화합니다.
 */
export async function clearToken(page: Page): Promise<void> {
  await page.addInitScript(() => {
    window.localStorage.removeItem('accessToken')
  })
}

/**
 * 인증 완료된 사용자의 JWT 토큰 문자열을 반환합니다.
 * verify mock에서 응답으로 반환할 토큰 값이 필요할 때 사용합니다.
 */
export function buildVerifiedJwt(): string {
  const now = nowSeconds()
  return buildFakeJwt({
    sub: '1',
    emailVerified: true,
    iat: now,
    exp: now + 3600,
  })
}
