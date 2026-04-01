interface JwtPayload {
  sub: string
  emailVerified: boolean
  iat: number
  exp: number
}

export function decodeJwt(token: string): JwtPayload {
  const base64Url = token.split('.')[1]
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
  const json = decodeURIComponent(
    atob(base64)
      .split('')
      .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
      .join(''),
  )
  return JSON.parse(json)
}

export function isEmailVerified(token: string): boolean {
  try {
    return decodeJwt(token).emailVerified === true
  } catch {
    return false
  }
}
