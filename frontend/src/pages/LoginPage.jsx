import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { register as apiRegister } from '../api/client.js'
import Logo from '../components/Logo.jsx'

const ROLES = ['DOCTOR', 'RADIOLOGIST', 'NURSE', 'ADMIN']
const LOGIN_INIT    = { email: '', password: '' }
const REGISTER_INIT = { name: '', email: '', password: '', confirmPassword: '', role: 'DOCTOR' }

export default function LoginPage() {
  const { login } = useAuth()
  const navigate   = useNavigate()

  const [tab, setTab]         = useState('login')
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')
  const [popup, setPopup]     = useState('')
  const [loginForm, setLoginForm]       = useState(LOGIN_INIT)
  const [registerForm, setRegisterForm] = useState(REGISTER_INIT)

  const changeLogin    = (e) => setLoginForm((f)    => ({ ...f, [e.target.name]: e.target.value }))
  const changeRegister = (e) => setRegisterForm((f) => ({ ...f, [e.target.name]: e.target.value }))
  const switchTab = (t) => { setTab(t); setError('') }

  const handleLogin = async (e) => {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      await login(loginForm.email, loginForm.password)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message ?? 'Invalid email or password.')
    } finally { setLoading(false) }
  }

  const handleRegister = async (e) => {
    e.preventDefault()
    if (registerForm.password !== registerForm.confirmPassword) {
      setError('Passwords do not match.'); return
    }
    if (registerForm.password.length < 8) {
      setError('Password must be at least 8 characters.'); return
    }
    setLoading(true); setError('')
    try {
      await apiRegister(registerForm.name, registerForm.email, registerForm.password, registerForm.role)
      const message = 'Account created successfully. You can now sign in.'
      setPopup(message)
      setRegisterForm(REGISTER_INIT)
      switchTab('login')
      setLoginForm((f) => ({ ...f, email: registerForm.email }))
      window.setTimeout(() => setPopup(''), 4000)
    } catch (err) {
      setError(getErrorMessage(err, 'Registration failed. Try a different email.'))
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen flex relative overflow-hidden">
      {popup && (
        <div className="fixed top-5 right-5 z-50 max-w-sm rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 shadow-xl shadow-emerald-100/70 flex items-start gap-2.5">
          <svg className="w-4 h-4 mt-0.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div className="flex-1">
            <p className="font-semibold">Registration complete</p>
            <p className="mt-0.5">{popup}</p>
          </div>
          <button
            type="button"
            onClick={() => setPopup('')}
            className="ml-2 text-emerald-500 hover:text-emerald-700 transition-colors"
            aria-label="Dismiss notification"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      )}

      {/* Near-white gradient background — same as Dashboard / PatientPage */}
      <div
        className="fixed inset-0 -z-10"
        style={{ background: 'linear-gradient(160deg, #ffffff 0%, #f8fafc 55%, #f0f5ff 100%)' }}
      />
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute -top-32 -right-32 w-[600px] h-[600px] bg-blue-100/20 rounded-full blur-3xl" />
        <div className="absolute bottom-0 -left-24 w-[500px] h-[500px] bg-indigo-100/15 rounded-full blur-3xl" />
      </div>

      {/* ── Left panel (brand / dark accent) ── */}
      <div className="hidden lg:flex lg:w-[52%] relative flex-col items-start justify-center p-14 overflow-hidden">
        {/* Dark glass inset */}
        <div className="absolute inset-0 rounded-r-[3rem]"
          style={{
            background: 'linear-gradient(135deg, rgba(30,58,138,0.92) 0%, rgba(30,27,75,0.95) 55%, rgba(15,23,42,0.97) 100%)',
            backdropFilter: 'blur(32px)',
          }}
        />
        {/* Orbs */}
        <div className="absolute -top-20 -left-20 w-80 h-80 bg-blue-500/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-10 right-0 w-64 h-64 bg-violet-500/15 rounded-full blur-3xl pointer-events-none" />

        {/* Single centred content block */}
        <div className="relative z-10 flex flex-col gap-8">
          <Logo size="md" textColor="text-white" />

          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/10 border border-white/15 text-blue-200 text-xs font-medium mb-6">
              <span className="w-1.5 h-1.5 rounded-full bg-teal-400 animate-pulse" />
              AI-Powered Cardiac Platform
            </div>
            <h1 className="text-4xl font-bold text-white leading-[1.2] mb-5">
              Multimodal Cardiac<br />Decision Support
            </h1>
            <p className="text-blue-200/80 text-base leading-relaxed max-w-sm mb-10">
              ECG signal analysis and coronary vessel analysis for modern Cath Lab clinical workflows.
            </p>
            <div className="space-y-3">
              {[
                { label: 'Automated ECG Interpretation',   icon: '⚡' },
                { label: 'Coronary Vessel Analysis',       icon: '🔬' },
                { label: 'Comprehensive Clinical Reports', icon: '📋' },
              ].map((f) => (
                <div key={f.label} className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center text-sm flex-shrink-0">
                    {f.icon}
                  </div>
                  <span className="text-blue-100/80 text-sm">{f.label}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

      </div>

      {/* ── Right panel (form) — white glass, same as other pages ── */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-[400px]">

          {/* Mobile logo */}
          <div className="lg:hidden mb-8">
            <Logo size="sm" textColor="text-slate-800" />
          </div>

          {/* Form card — same glass-card as Dashboard / PatientPage */}
          <div className="glass-card p-8">

            {/* Tab switcher */}
            <div
              className="flex rounded-xl p-1 mb-7 gap-1 border"
              style={{ background: 'rgba(248,250,252,0.7)', borderColor: 'rgba(255,255,255,0.8)' }}
            >
              {['login', 'register'].map((t) => (
                <button
                  key={t}
                  onClick={() => switchTab(t)}
                  className={`flex-1 py-2 text-sm font-medium rounded-lg transition-all duration-150 ${
                    tab === t
                      ? 'bg-white text-slate-900 shadow-sm border border-slate-200/60'
                      : 'text-slate-500 hover:text-slate-700'
                  }`}
                >
                  {t === 'login' ? 'Sign in' : 'Register'}
                </button>
              ))}
            </div>

            {/* Alerts */}
            {error && (
              <div className="mb-5 flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50/80 border border-red-200/70 text-sm text-red-700">
                <svg className="w-4 h-4 mt-0.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                </svg>
                {error}
              </div>
            )}
            {/* Login form */}
            {tab === 'login' && (
              <>
                <div className="mb-6">
                  <h2 className="text-2xl font-bold text-slate-900 mb-1">Welcome back</h2>
                  <p className="text-sm text-slate-500">Enter your credentials to continue.</p>
                </div>
                <form onSubmit={handleLogin} className="space-y-4">
                  <FormField label="Email address">
                    <FormInput name="email" type="email" autoComplete="email" required
                      value={loginForm.email} onChange={changeLogin} placeholder="you@hospital.org" />
                  </FormField>
                  <FormField label="Password">
                    <FormInput name="password" type="password" autoComplete="current-password" required
                      value={loginForm.password} onChange={changeLogin} placeholder="••••••••" />
                  </FormField>
                  <div className="pt-1">
                    <SubmitBtn loading={loading} label="Sign in" loadingLabel="Signing in…" />
                  </div>
                </form>
                <p className="mt-6 text-center text-sm text-slate-500">
                  No account?{' '}
                  <button onClick={() => switchTab('register')} className="text-blue-600 font-semibold hover:text-blue-700 transition-colors">
                    Register here
                  </button>
                </p>
              </>
            )}

            {/* Register form */}
            {tab === 'register' && (
              <>
                <div className="mb-5">
                  <h2 className="text-2xl font-bold text-slate-900 mb-1">Create account</h2>
                  <p className="text-sm text-slate-500">Register your clinical credentials.</p>
                </div>
                <form onSubmit={handleRegister} className="space-y-4">
                  <FormField label="Full name">
                    <FormInput name="name" type="text" autoComplete="name" required
                      value={registerForm.name} onChange={changeRegister} placeholder="Dr. Jane Smith" />
                  </FormField>
                  <FormField label="Email address">
                    <FormInput name="email" type="email" autoComplete="email" required
                      value={registerForm.email} onChange={changeRegister} placeholder="you@hospital.org" />
                  </FormField>
                  <FormField label="Role">
                    <select name="role" value={registerForm.role} onChange={changeRegister}
                      className="block w-full px-3.5 py-2.5 text-sm text-slate-900 rounded-xl border transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400"
                      style={{ background: 'rgba(255,255,255,0.55)', backdropFilter: 'blur(8px)', borderColor: 'rgba(255,255,255,0.7)' }}>
                      {ROLES.map((r) => <option key={r} value={r}>{r.charAt(0) + r.slice(1).toLowerCase()}</option>)}
                    </select>
                  </FormField>
                  <div className="grid grid-cols-2 gap-3">
                    <FormField label="Password">
                      <FormInput name="password" type="password" autoComplete="new-password" required minLength={8}
                        value={registerForm.password} onChange={changeRegister} placeholder="Min. 8 chars" />
                    </FormField>
                    <FormField label="Confirm">
                      <FormInput name="confirmPassword" type="password" autoComplete="new-password" required
                        value={registerForm.confirmPassword} onChange={changeRegister} placeholder="••••••••" />
                    </FormField>
                  </div>
                  <div className="pt-1">
                    <SubmitBtn loading={loading} label="Create account" loadingLabel="Creating…" />
                  </div>
                </form>
                <p className="mt-6 text-center text-sm text-slate-500">
                  Already have an account?{' '}
                  <button onClick={() => switchTab('login')} className="text-blue-600 font-semibold hover:text-blue-700 transition-colors">
                    Sign in
                  </button>
                </p>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

function getErrorMessage(err, fallback) {
  const data = err.response?.data
  if (typeof data === 'string') return data
  if (data?.message) return data.message
  if (data?.errors && typeof data.errors === 'object') {
    return Object.values(data.errors).filter(Boolean).join(' ')
  }
  return fallback
}

function FormField({ label, children }) {
  return (
    <div>
      <label className="block text-xs font-semibold text-slate-500 mb-1.5 uppercase tracking-wider">{label}</label>
      {children}
    </div>
  )
}

function FormInput({ ...props }) {
  return (
    <input
      {...props}
      className="block w-full px-3.5 py-2.5 text-sm text-slate-900 rounded-xl border transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400 placeholder-slate-400"
      style={{ background: 'rgba(255,255,255,0.55)', backdropFilter: 'blur(8px)', borderColor: 'rgba(255,255,255,0.7)' }}
    />
  )
}

function SubmitBtn({ loading, label, loadingLabel }) {
  return (
    <button type="submit" disabled={loading} className="btn-primary w-full justify-center py-2.5">
      {loading
        ? <><span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />{loadingLabel}</>
        : label}
    </button>
  )
}
