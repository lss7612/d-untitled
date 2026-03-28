import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

export default function AuthCallbackPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  useEffect(() => {
    const token = searchParams.get('token')
    const error = searchParams.get('error')

    if (error) {
      navigate(`/login?error=${error}`)
      return
    }

    if (token) {
      localStorage.setItem('accessToken', token)
      navigate('/home', { replace: true })
    } else {
      navigate('/login?error=missing_token')
    }
  }, [navigate, searchParams])

  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-950">
      <p className="text-zinc-400 text-sm">로그인 처리 중...</p>
    </div>
  )
}
