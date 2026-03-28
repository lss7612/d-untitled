import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Menu, X } from 'lucide-react'
import { motion, AnimatePresence } from 'motion/react'
import { toast } from 'sonner'

const NAV_ITEMS = [
  { label: '모든 동호회', path: '/home', implemented: true },
  { label: '내 동호회', path: '/clubs', implemented: true },
  { label: '내 프로필', path: '/profile', implemented: false },
  { label: '포인트 내역', path: '/points', implemented: false },
]

export default function AppHeader() {
  const navigate = useNavigate()
  const location = useLocation()
  const [open, setOpen] = useState(false)

  function handleLogout() {
    localStorage.removeItem('accessToken')
    setOpen(false)
    navigate('/login', { replace: true })
  }

  function handleNavItem(path: string, implemented: boolean) {
    if (!implemented) {
      toast.info('준비 중입니다.')
      setOpen(false)
      return
    }
    setOpen(false)
    navigate(path)
  }

  return (
    <>
      <header className="flex items-center justify-between px-6 py-4 border-b border-zinc-900">
        <button
          onClick={() => navigate('/home')}
          className="text-base font-semibold tracking-tight text-zinc-200 hover:text-zinc-100 transition-colors"
        >
          MOMO
        </button>
        <button
          onClick={() => setOpen(true)}
          className="text-zinc-500 hover:text-zinc-300 transition-colors p-1"
          aria-label="메뉴 열기"
        >
          <Menu size={20} />
        </button>
      </header>

      <AnimatePresence>
        {open && (
          <>
            {/* 배경 오버레이 */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              className="fixed inset-0 z-40 bg-black/50"
              onClick={() => setOpen(false)}
            />

            {/* 슬라이드 메뉴 */}
            <motion.nav
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'tween', duration: 0.25 }}
              className="fixed top-0 right-0 h-full w-64 z-50 bg-zinc-950 border-l border-zinc-900 flex flex-col"
            >
              {/* 메뉴 헤더 */}
              <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-900">
                <span className="text-sm font-medium text-zinc-400">메뉴</span>
                <button
                  onClick={() => setOpen(false)}
                  className="text-zinc-500 hover:text-zinc-300 transition-colors p-1"
                  aria-label="메뉴 닫기"
                >
                  <X size={18} />
                </button>
              </div>

              {/* 메뉴 항목 */}
              <div className="flex-1 py-4">
                {NAV_ITEMS.map((item) => {
                  const isActive = location.pathname === item.path
                  return (
                    <button
                      key={item.path}
                      onClick={() => handleNavItem(item.path, item.implemented)}
                      className={[
                        'w-full text-left px-6 py-3 text-sm transition-colors',
                        item.implemented
                          ? isActive
                            ? 'text-zinc-100 bg-zinc-900/60'
                            : 'text-zinc-300 hover:text-zinc-100 hover:bg-zinc-900/40'
                          : 'text-zinc-600 cursor-default',
                      ].join(' ')}
                    >
                      {item.label}
                      {!item.implemented && (
                        <span className="ml-2 text-xs text-zinc-700">준비중</span>
                      )}
                    </button>
                  )
                })}
              </div>

              {/* 로그아웃 */}
              <div className="border-t border-zinc-900 px-6 py-4">
                <button
                  onClick={handleLogout}
                  className="text-sm text-zinc-600 hover:text-zinc-400 transition-colors"
                >
                  로그아웃
                </button>
              </div>
            </motion.nav>
          </>
        )}
      </AnimatePresence>
    </>
  )
}
