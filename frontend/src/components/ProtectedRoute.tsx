import { Navigate, Outlet } from 'react-router-dom'
import { isEmailVerified } from '@/lib/jwt'

export default function ProtectedRoute() {
  const token = localStorage.getItem('accessToken')
  if (!token) return <Navigate to="/login" replace />
  if (!isEmailVerified(token)) return <Navigate to="/auth/email-verify" replace />
  return <Outlet />
}
