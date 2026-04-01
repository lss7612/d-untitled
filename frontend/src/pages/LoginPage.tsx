import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { motion, useReducedMotion } from 'motion/react'
import { Bell, Users, Trophy } from 'lucide-react'
import { ShimmerButton } from '@/components/ui/shimmer-button'
import { AnimatedGridPattern } from '@/components/ui/animated-grid-pattern'
import { cn } from '@/lib/utils'
import { isEmailVerified } from '@/lib/jwt'

const FEATURES = [
  { icon: Bell, label: '스마트\n알림' },
  { icon: Users, label: '멤버\n관리' },
  { icon: Trophy, label: '포인트\n시스템' },
]

const ERROR_MESSAGES: Record<string, string> = {
  domain_not_allowed: '등록된 회사 이메일만 이용 가능합니다.',
  auth_failed: 'Google 인증에 실패했습니다. 다시 시도해주세요.',
  server_error: '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
}

export default function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const shouldReduceMotion = useReducedMotion()

  const errorParam = searchParams.get('error')
  const errorMessage = errorParam ? (ERROR_MESSAGES[errorParam] ?? '알 수 없는 오류가 발생했습니다.') : null

  // 이미 인증 완료된 사용자는 /home으로 리다이렉트
  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (token && isEmailVerified(token)) {
      navigate('/home', { replace: true })
    }
  }, [navigate])

  function handleGoogleLogin() {
    window.location.href = 'http://localhost:8080/oauth2/authorization/google'
  }

  const fadeUp = (delay: number) =>
    shouldReduceMotion
      ? {}
      : {
          initial: { opacity: 0, y: 16 },
          animate: { opacity: 1, y: 0 },
          transition: { duration: 0.5, ease: 'easeOut', delay },
        }

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center bg-zinc-950 overflow-hidden px-6">
      {/* 배경 */}
      <AnimatedGridPattern
        numSquares={30}
        maxOpacity={0.06}
        duration={3}
        strokeDasharray={4}
        className={cn(
          'absolute inset-0 h-full w-full text-zinc-500',
          '[mask-image:radial-gradient(600px_circle_at_center,white,transparent)]',
        )}
      />

      {/* 콘텐츠 */}
      <div className="relative z-10 flex flex-col items-center gap-10 w-full max-w-sm">

        {/* 워드마크 */}
        <motion.div className="flex flex-col items-center gap-1.5" {...fadeUp(0)}>
          <h1 className="text-3xl font-semibold tracking-tight text-zinc-100">MOMO</h1>
          <p className="text-[11px] text-zinc-500 tracking-widest uppercase">
            사내 동호회 관리 플랫폼
          </p>
        </motion.div>

        {/* Hero 헤드라인 */}
        <motion.div className="text-center" {...fadeUp(0.1)}>
          <p className="text-xl font-medium text-zinc-200 leading-snug">
            동호회 운영을<br />더 스마트하게
          </p>
          <p className="mt-2 text-sm text-zinc-500 leading-relaxed">
            반복적인 행정 업무를 자동화하고<br />활동에 집중하세요.
          </p>
        </motion.div>

        {/* Feature Highlights */}
        <motion.div className="grid grid-cols-3 gap-3 w-full" {...fadeUp(0.2)}>
          {FEATURES.map(({ icon: Icon, label }) => (
            <div
              key={label}
              className="flex flex-col items-center gap-2.5 rounded-xl bg-zinc-900/60 border border-zinc-800/50 px-3 py-4"
            >
              <div className="rounded-lg bg-zinc-800/80 p-2">
                <Icon size={16} className="text-zinc-400" strokeWidth={1.5} />
              </div>
              <p className="text-[11px] text-zinc-500 text-center leading-relaxed whitespace-pre-line">
                {label}
              </p>
            </div>
          ))}
        </motion.div>

        {/* 로그인 카드 */}
        <motion.div
          className="w-full rounded-2xl bg-white/5 backdrop-blur-md border border-white/10 shadow-[0_8px_32px_rgba(0,0,0,0.4)] px-8 py-8 flex flex-col items-center gap-5"
          {...fadeUp(0.3)}
        >
          <div className="text-center">
            <p className="text-sm font-medium text-zinc-200">로그인</p>
            <p className="text-xs text-zinc-500 mt-1">회사 Google 계정으로 시작하세요</p>
          </div>

          {errorMessage && (
            <p role="alert" className="w-full rounded-lg bg-red-950/40 border border-red-800/40 px-3 py-2 text-xs text-red-400 text-center">
              {errorMessage}
            </p>
          )}

          <ShimmerButton
            onClick={handleGoogleLogin}
            className="w-full h-11 px-6 gap-3 text-sm font-medium justify-center"
            background="rgba(39, 39, 42, 1)"
            shimmerColor="#71717a"
          >
            <GoogleIcon />
            Google 계정으로 로그인
          </ShimmerButton>

          <p className="text-[11px] text-zinc-600 text-center leading-5">
            kr.doubledown.com · afewgoodsoft.com<br />doubleugames.com
          </p>
        </motion.div>

        {/* 푸터 */}
        <motion.p className="text-[11px] text-zinc-700" {...fadeUp(0.4)}>
          © DoubleU Games / © Double Down Interactive
        </motion.p>
      </div>
    </div>
  )
}

function GoogleIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="16" height="16" aria-hidden="true">
      <path fill="#FFC107" d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z" />
      <path fill="#FF3D00" d="m6.306 14.691 6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z" />
      <path fill="#4CAF50" d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238A11.91 11.91 0 0 1 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z" />
      <path fill="#1976D2" d="M43.611 20.083H42V20H24v8h11.303a12.04 12.04 0 0 1-4.087 5.571l.003-.002 6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z" />
    </svg>
  )
}
