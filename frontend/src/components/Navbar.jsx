import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

const ROLE_COLORS = {
  DOCTOR:      'bg-blue-100 text-blue-700',
  RADIOLOGIST: 'bg-purple-100 text-purple-700',
  NURSE:       'bg-green-100 text-green-700',
  ADMIN:       'bg-orange-100 text-orange-700',
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link to="/dashboard" className="flex items-center gap-3">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
              <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
            </div>
            <span className="text-lg font-semibold text-slate-800 tracking-tight">HeartSync</span>
          </Link>

          <div className="flex items-center gap-4">
            {user && (
              <>
                <span className={`badge ${ROLE_COLORS[user.role] ?? 'bg-slate-100 text-slate-700'}`}>
                  {user.role}
                </span>
                <span className="text-sm text-slate-600 hidden sm:block">{user.name}</span>
              </>
            )}
            <button
              onClick={handleLogout}
              className="btn-secondary text-xs px-3 py-1.5"
            >
              Sign out
            </button>
          </div>
        </div>
      </div>
    </header>
  )
}
