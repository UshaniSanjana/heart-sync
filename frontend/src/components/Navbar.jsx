import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import Logo from './Logo.jsx'

const ROLE_STYLES = {
  DOCTOR:      'bg-blue-100/70 text-blue-700 ring-1 ring-blue-300/60',
  RADIOLOGIST: 'bg-violet-100/70 text-violet-700 ring-1 ring-violet-300/60',
  NURSE:       'bg-emerald-100/70 text-emerald-700 ring-1 ring-emerald-300/60',
  ADMIN:       'bg-amber-100/70 text-amber-700 ring-1 ring-amber-300/60',
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <header
      className="sticky top-0 z-30"
      style={{
        background: 'rgba(255,255,255,0.72)',
        backdropFilter: 'blur(24px)',
        WebkitBackdropFilter: 'blur(24px)',
        borderBottom: '1px solid rgba(255,255,255,0.65)',
        boxShadow: '0 1px 12px rgba(148,163,184,0.12)',
      }}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-14">

          {/* Brand */}
          <Link to="/dashboard" className="group hover:opacity-90 transition-opacity">
            <Logo size="sm" textColor="text-slate-800" />
          </Link>

          {/* Right side */}
          <div className="flex items-center gap-3">
            {user && (
              <div className="flex items-center gap-3">
                <span className={`badge text-[11px] font-semibold px-2.5 py-1 ${ROLE_STYLES[user.role] ?? 'bg-slate-100/70 text-slate-600'}`}>
                  {user.role}
                </span>
                <div className="hidden sm:flex items-center gap-2">
                  <div className="w-7 h-7 rounded-full bg-gradient-to-br from-slate-400 to-slate-600 flex items-center justify-center text-white text-xs font-semibold shadow-sm">
                    {user.name?.[0]?.toUpperCase()}
                  </div>
                  <span className="text-sm font-medium text-slate-700">{user.name}</span>
                </div>
              </div>
            )}
            <button
              onClick={() => { logout(); navigate('/login') }}
              className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-slate-800 px-3 py-1.5 rounded-lg transition-all"
              style={{ background: 'rgba(255,255,255,0.55)', backdropFilter: 'blur(8px)', border: '1px solid rgba(255,255,255,0.7)' }}
            >
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
              Sign out
            </button>
          </div>

        </div>
      </div>
    </header>
  )
}
