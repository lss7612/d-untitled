import { test, expect } from '@playwright/test'
import { setVerifiedToken, setUnverifiedToken, clearToken } from '../helpers/auth'

/**
 * 인증 가드(ProtectedRoute) E2E 테스트
 *
 * AC 출처:
 * - docs/plan/02-feature-spec.md (F-00)
 *   - 토큰 없이 보호 라우트(/home, /clubs, /muje) 접근 → /login으로 리다이렉트
 *   - 토큰 있지만 이메일 미인증 상태로 보호 라우트 접근 → /auth/email-verify로 리다이렉트
 *   - JWT 만료 후 보호 페이지 접근 → /login으로 리다이렉트
 *
 * - docs/plan/03-page-spec.md (PAGE-01)
 *   - 이미 로그인 + 이메일 인증 완료된 사용자가 /login 접근 → /home으로 자동 리다이렉트
 */

test.describe('AUTH-GUARD-01: 토큰 없이 보호 라우트 접근', () => {
  test.beforeEach(async ({ page }) => {
    await clearToken(page)
  })

  test('토큰 없이 /home 접근 → /login 리다이렉트', async ({ page }) => {
    // AC: 토큰 없이 보호 라우트(/home) 접근 → /login으로 리다이렉트
    await page.goto('/home')
    await expect(page).toHaveURL(/\/login/, { timeout: 5000 })
  })

  test('토큰 없이 /clubs 접근 → /login 리다이렉트', async ({ page }) => {
    // AC: 토큰 없이 보호 라우트(/clubs) 접근 → /login으로 리다이렉트
    await page.goto('/clubs')
    await expect(page).toHaveURL(/\/login/, { timeout: 5000 })
  })

  test('토큰 없이 /muje 접근 → /login 리다이렉트', async ({ page }) => {
    // AC: 토큰 없이 보호 라우트(/muje) 접근 → /login으로 리다이렉트
    await page.goto('/muje')
    await expect(page).toHaveURL(/\/login/, { timeout: 5000 })
  })
})

test.describe('AUTH-GUARD-02: 이메일 미인증 토큰으로 보호 라우트 접근', () => {
  test('emailVerified=false 토큰으로 /home 접근 → /auth/email-verify 리다이렉트', async ({ page }) => {
    // AC: 토큰 있지만 이메일 미인증 상태로 보호 라우트 접근 → /auth/email-verify로 리다이렉트
    await setUnverifiedToken(page)
    await page.goto('/home')
    await expect(page).toHaveURL(/\/auth\/email-verify/, { timeout: 5000 })
  })
})

test.describe('AUTH-GUARD-03: 이메일 인증 완료 토큰으로 보호 라우트 접근', () => {
  test('emailVerified=true 토큰으로 /home 접근 → 정상 접근 (리다이렉트 없음)', async ({ page }) => {
    // AC: 이미 이메일 인증 완료된 사용자 → 보호 라우트 정상 접근
    await setVerifiedToken(page)
    await page.goto('/home')
    // /login 또는 /auth/email-verify로 리다이렉트되지 않아야 함
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page).not.toHaveURL(/\/auth\/email-verify/)
    await expect(page).toHaveURL(/\/home/)
  })
})

test.describe('AUTH-GUARD-04: 인증 완료 사용자의 /login 접근', () => {
  test('emailVerified=true 토큰으로 /login 접근 → /home 리다이렉트', async ({ page }) => {
    // AC: 이미 로그인 + 이메일 인증 완료된 사용자가 /login 접근 → /home으로 자동 리다이렉트
    await setVerifiedToken(page)
    await page.goto('/login')
    await expect(page).toHaveURL(/\/home/, { timeout: 5000 })
  })
})
