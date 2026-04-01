import { useMutation } from '@tanstack/react-query'
import { sendVerifyCode, verifyCode } from '@/api/auth'

export function useSendVerifyCode() {
  return useMutation({
    mutationFn: sendVerifyCode,
  })
}

export function useVerifyCode() {
  return useMutation({
    mutationFn: (code: string) => verifyCode(code),
  })
}
