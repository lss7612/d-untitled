export interface MemberResponse {
  id: number
  email: string
  name: string
  picture: string
  role: string
}

export async function fetchMe(): Promise<MemberResponse> {
  const token = localStorage.getItem('accessToken')
  const res = await fetch('/api/v1/members/me', {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error('Unauthorized')
  const body = await res.json()
  return body.data
}
