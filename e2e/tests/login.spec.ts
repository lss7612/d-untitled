import { test, expect } from '@playwright/test'
import { setVerifiedToken, clearToken } from '../helpers/auth'

/**
 * PAGE-01 로그인 페이지 E2E 테스트
 *
 * AC 출처:
 * - docs/plan/03-page-spec.md  (PAGE-01)
 * - docs/plan/02-feature-spec.md (F-00)
 */

test.describe('LOGIN-01: 로그인 페이지 렌더링', () => {
  test.beforeEach(async ({ page }) => {
    await clearToken(page)
    await page.goto('/login')
  })

  test('MOMO 타이틀이 표시되어야 한다', async ({ page }) => {
    // AC: 서비스 로고 / 타이틀 표시
    await expect(page.getByRole('heading', { name: 'MOMO' })).toBeVisible()
  })

  test('"Google 계정으로 로그인" 버튼이 표시되어야 한다', async ({ page }) => {
    // AC: "Google 계정으로 로그인" 버튼 존재
    await expect(page.getByRole('button', { name: /Google 계정으로 로그인/ })).toBeVisible()
  })

  test('허용 도메인 안내 텍스트가 표시되어야 한다', async ({ page }) => {
    // AC: 회사 이메일 도메인 제한 안내 문구 (kr.doubledown.com / afewgoodsoft.com / doubleugames.com)
    await expect(page.getByText(/kr\.doubledown\.com/)).toBeVisible()
    await expect(page.getByText(/afewgoodsoft\.com/)).toBeVisible()
    await expect(page.getByText(/doubleugames\.com/)).toBeVisible()
  })
})

test.describe('LOGIN-02: Google 로그인 버튼 네비게이션', () => {
  test.beforeEach(async ({ page }) => {
    await clearToken(page)
    await page.goto('/login')
  })

  test('Google 로그인 버튼 클릭 시 OAuth 엔드포인트로 이동해야 한다', async ({ page }) => {
    // AC: "Google 계정으로 로그인" 버튼 클릭 → Google OAuth 인증 화면으로 이동
    // window.location.href 를 직접 변경하므로 navigation 을 감지합니다.
    const [request] = await Promise.all([
      page.waitForURL('http://localhost:8080/oauth2/authorization/google', {
        timeout: 5000,
        waitUntil: 'commit',
      }).catch(() => null), // 실제 서버 없으면 실패할 수 있으므로 URL만 확인
      page.getByRole('button', { name: /Google 계정으로 로그인/ }).click(),
    ])

    // 네비게이션 목적지가 올바른 OAuth URL인지 확인
    // 실제 서버가 없으면 ERR_CONNECTION_REFUSED 로 실패하지만 URL은 변경됩니다.
    await expect(page).toHaveURL(/localhost:8080\/oauth2\/authorization\/google/)
  })
})

test.describe('LOGIN-03: 에러 파라미터 처리', () => {
  test.beforeEach(async ({ page }) => {
    await clearToken(page)
  })

  test('?error=domain_not_allowed → 도메인 미허용 에러 메시지가 표시되어야 한다', async ({ page }) => {
    // AC: 미허용 도메인 계정으로 인증 시도 → "등록된 회사 이메일만 이용 가능합니다" 에러 메시지
    await page.goto('/login?error=domain_not_allowed')
    await expect(page.getByText(/등록된 회사 이메일만 이용 가능합니다/)).toBeVisible()
  })

  test('?error=auth_failed → 인증 실패 에러 메시지가 표시되어야 한다', async ({ page }) => {
    // AC: Google OAuth 취소 또는 실패 → 에러 메시지 표시 + 로그인 페이지 유지
    await page.goto('/login?error=auth_failed')
    await expect(page.getByText(/인증에 실패했습니다|auth_failed/i)).toBeVisible()
  })

  test('에러 발생 후에도 로그인 페이지가 유지되어야 한다', async ({ page }) => {
    await page.goto('/login?error=auth_failed')
    // 로그인 버튼이 여전히 표시되는지 확인 (페이지 유지 검증)
    await expect(page.getByRole('button', { name: /Google 계정으로 로그인/ })).toBeVisible()
    await expect(page).toHaveURL(/\/login/)
  })
})

test.describe('LOGIN-04: 인증 완료 사용자 리다이렉트', () => {
  test('이미 인증된 사용자가 /login 접근 시 /home으로 리다이렉트되어야 한다', async ({ page }) => {
    // AC: 이미 로그인 + 이메일 인증 완료된 사용자가 /login 접근 → /home으로 자동 리다이렉트
    await setVerifiedToken(page)
    await page.goto('/login')
    await expect(page).toHaveURL(/\/home/)
  })
})
