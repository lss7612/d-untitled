import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ArrowLeft, Plus, X } from 'lucide-react'
import AppHeader from '@/components/AppHeader'
import { getConfig, updateConfig } from '@/api/appConfig'

export default function DeveloperWhitelistPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [input, setInput] = useState('')

  const { data: config, isLoading } = useQuery({
    queryKey: ['appConfig', 'WHITE_LIST'],
    queryFn: () => getConfig('WHITE_LIST'),
  })

  const emails: string[] = (() => {
    try { return JSON.parse(config?.contents ?? '[]') } catch { return [] }
  })()

  const mutation = useMutation({
    mutationFn: (next: string[]) => updateConfig('WHITE_LIST', next),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['appConfig', 'WHITE_LIST'] })
      toast.success('화이트리스트가 저장되었습니다.')
    },
    onError: () => toast.error('저장에 실패했습니다.'),
  })

  function handleAdd() {
    const email = input.trim().toLowerCase()
    if (!email) return
    if (emails.includes(email)) { toast.info('이미 등록된 이메일입니다.'); return }
    mutation.mutate([...emails, email])
    setInput('')
  }

  function handleRemove(email: string) {
    mutation.mutate(emails.filter((e) => e !== email))
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />
      <main className="max-w-2xl mx-auto px-6 py-14">
        <button
          onClick={() => navigate('/muje')}
          className="flex items-center gap-1.5 text-sm text-zinc-500 hover:text-zinc-300 mb-8 transition-colors"
        >
          <ArrowLeft size={14} />
          무제 대시보드
        </button>

        <h2 className="text-xl font-semibold mb-1 text-zinc-200">화이트리스트 관리</h2>
        <p className="text-sm text-zinc-500 mb-8">
          도메인 제한 없이 로그인을 허용할 이메일 목록입니다.
        </p>

        <div className="flex gap-2 mb-6">
          <input
            type="email"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleAdd()}
            placeholder="추가할 이메일 입력"
            className="flex-1 rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-sm text-zinc-200 placeholder-zinc-500 focus:outline-none focus:border-purple-500"
          />
          <button
            onClick={handleAdd}
            disabled={mutation.isPending}
            className="flex items-center gap-1.5 rounded-lg bg-purple-700 hover:bg-purple-600 px-4 py-2.5 text-sm font-medium text-white transition-colors disabled:opacity-50"
          >
            <Plus size={14} />
            추가
          </button>
        </div>

        {isLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}

        {!isLoading && emails.length === 0 && (
          <p className="text-sm text-zinc-500">등록된 이메일이 없습니다.</p>
        )}

        <ul className="space-y-2">
          {emails.map((email) => (
            <li
              key={email}
              className="flex items-center justify-between rounded-xl border border-zinc-800/40 bg-zinc-900/50 px-4 py-3"
            >
              <span className="text-sm text-zinc-300">{email}</span>
              <button
                onClick={() => handleRemove(email)}
                disabled={mutation.isPending}
                className="text-zinc-500 hover:text-red-400 transition-colors disabled:opacity-50"
              >
                <X size={14} />
              </button>
            </li>
          ))}
        </ul>
      </main>
    </div>
  )
}
