import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, useReducedMotion } from 'motion/react'
import { toast } from 'sonner'
import { InputOTP, InputOTPGroup, InputOTPSlot } from '@/components/ui/input-otp'
import { ShimmerButton } from '@/components/ui/shimmer-button'
import { AnimatedGridPattern } from '@/components/ui/animated-grid-pattern'
import { cn } from '@/lib/utils'
import { useSendVerifyCode, useVerifyCode } from '@/hooks/useEmailVerify'
import { isEmailVerified } from '@/lib/jwt'

const COOLDOWN_SECONDS = 60

export default function EmailVerifyPage() {
  const navigate = useNavigate()
  const shouldReduceMotion = useReducedMotion()

  const [code, setCode] = useState('')
  const [cooldown, setCooldown] = useState(0)
  const [isLocked, setIsLocked] = useState(false)
  const [lockMessage, setLockMessage] = useState('')

  const sendCode = useSendVerifyCode()
  const verify = useVerifyCode()
  const hasSentRef = useRef(false)

  // Redirect if already verified
  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token || isEmailVerified(token)) {
      navigate('/home', { replace: true })
    }
  }, [navigate])

  // Send code on mount (ref guard prevents StrictMode double-fire)
  useEffect(() => {
    if (hasSentRef.current) return
    hasSentRef.current = true
    sendCode.mutate(undefined, {
      onSuccess: () => {
        setCooldown(COOLDOWN_SECONDS)
        toast.success('인증 코드가 발송되었습니다.')
      },
      onError: (err) => {
        toast.error(err.message)
      },
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Cooldown timer
  useEffect(() => {
    if (cooldown <= 0) return
    const timer = setInterval(() => setCooldown((prev) => prev - 1), 1000)
    return () => clearInterval(timer)
  }, [cooldown])

  const handleResend = useCallback(() => {
    if (cooldown > 0 || sendCode.isPending) return
    sendCode.mutate(undefined, {
      onSuccess: () => {
        setCooldown(COOLDOWN_SECONDS)
        setCode('')
        toast.success('인증 코드가 재발송되었습니다.')
      },
      onError: (err) => {
        toast.error(err.message)
      },
    })
  }, [cooldown, sendCode])

  const handleSubmit = useCallback(() => {
    if (code.length !== 6 || verify.isPending || isLocked) return
    verify.mutate(code, {
      onSuccess: (data) => {
        localStorage.setItem('accessToken', data.token)
        toast.success('이메일 인증이 완료되었습니다.')
        navigate('/home', { replace: true })
      },
      onError: (err) => {
        if (err.message.includes('잠금')) {
          setIsLocked(true)
          setLockMessage(err.message)
        } else {
          toast.error(err.message)
        }
        setCode('')
      },
    })
  }, [code, verify, navigate, isLocked])

  const fadeUp = (delay: number) =>
    shouldReduceMotion
      ? {}
      : {
          initial: { opacity: 0, y: 16 },
          animate: { opacity: 1, y: 0 },
          transition: { duration: 0.5, ease: 'easeOut' as const, delay },
        }

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center bg-zinc-950 overflow-hidden px-6">
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

      <div className="relative z-10 flex flex-col items-center gap-10 w-full max-w-sm">
        {/* Header */}
        <motion.div className="flex flex-col items-center gap-1.5" {...fadeUp(0)}>
          <h1 className="text-3xl font-semibold tracking-tight text-zinc-100">MOMO</h1>
          <p className="text-[11px] text-zinc-500 tracking-widest uppercase">
            이메일 인증
          </p>
        </motion.div>

        {/* Card */}
        <motion.div
          className="w-full rounded-2xl bg-white/5 backdrop-blur-md border border-white/10 shadow-[0_8px_32px_rgba(0,0,0,0.4)] px-8 py-8 flex flex-col items-center gap-6"
          {...fadeUp(0.1)}
        >
          <div className="text-center">
            <p className="text-sm font-medium text-zinc-200">인증 코드 입력</p>
            <p className="text-xs text-zinc-500 mt-1">
              회사 이메일로 발송된 6자리 코드를 입력하세요
            </p>
          </div>

          {isLocked && (
            <p role="alert" className="w-full rounded-lg bg-red-950/40 border border-red-800/40 px-3 py-2 text-xs text-red-400 text-center">
              {lockMessage}
            </p>
          )}

          {/* OTP Input */}
          <InputOTP
            maxLength={6}
            value={code}
            onChange={setCode}
            disabled={verify.isPending || isLocked}
          >
            <InputOTPGroup>
              <InputOTPSlot index={0} className="size-11 text-lg text-zinc-100" />
              <InputOTPSlot index={1} className="size-11 text-lg text-zinc-100" />
              <InputOTPSlot index={2} className="size-11 text-lg text-zinc-100" />
              <InputOTPSlot index={3} className="size-11 text-lg text-zinc-100" />
              <InputOTPSlot index={4} className="size-11 text-lg text-zinc-100" />
              <InputOTPSlot index={5} className="size-11 text-lg text-zinc-100" />
            </InputOTPGroup>
          </InputOTP>

          {/* Submit */}
          <ShimmerButton
            onClick={handleSubmit}
            disabled={code.length !== 6 || verify.isPending || isLocked}
            className="w-full h-11 px-6 text-sm font-medium justify-center"
            background="rgba(39, 39, 42, 1)"
            shimmerColor="#71717a"
          >
            {verify.isPending ? '확인 중...' : '인증하기'}
          </ShimmerButton>

          {/* Resend */}
          <button
            type="button"
            onClick={handleResend}
            disabled={cooldown > 0 || sendCode.isPending}
            className={cn(
              'text-xs transition-colors',
              cooldown > 0 || sendCode.isPending
                ? 'text-zinc-600 cursor-not-allowed'
                : 'text-zinc-400 hover:text-zinc-200 cursor-pointer',
            )}
          >
            {sendCode.isPending
              ? '발송 중...'
              : cooldown > 0
                ? `코드 재발송 (${cooldown}초)`
                : '코드 재발송'}
          </button>
        </motion.div>

        {/* Footer */}
        <motion.p className="text-[11px] text-zinc-700" {...fadeUp(0.2)}>
          인증 코드는 5분간 유효합니다
        </motion.p>
      </div>
    </div>
  )
}
