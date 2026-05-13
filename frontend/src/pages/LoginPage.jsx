import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { register as apiRegister } from '../api/client.js'

const ROLES = ['DOCTOR', 'RADIOLOGIST', 'NURSE', 'ADMIN']

const LOGIN_INIT    = { email: '', password: '' }
const REGISTER_INIT = { name: '', email: '', password: '', confirmPassword: '', role: 'DOCTOR' }

export default function LoginPage() {
  const { login } = useAuth()
  const navigate  = useNavigate()

  const [tab, setTab]       = useState('login')   // 'login' | 'register'
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')
  const [success, setSuccess] = useState('')

  const [loginForm, setLoginForm]       = useState(LOGIN_INIT)
  const [registerForm, setRegisterForm] = useState(REGISTER_INIT)

  const changeLogin    = (e) => setLoginForm((f)    => ({ ...f, [e.target.name]: e.target.value }))
  const changeRegister = (e) => setRegisterForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const switchTab = (t) => { setTab(t); setError(''); setSuccess('') }

  // ── Login submit ────────────────────────────────────────────────────────────
  const handleLogin = async (e) => {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      await login(loginForm.email, loginForm.password)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message ?? 'Invalid email or password.')
    } finally {
      setLoading(false)
    }
  }

  // ── Register submit ─────────────────────────────────────────────────────────
  const handleRegister = async (e) => {
    e.preventDefault()
    if (registerForm.password !== registerForm.confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    setLoading(true); setError('')
    try {
      await apiRegister(
        registerForm.name,
        registerForm.email,
        registerForm.password,
        registerForm.role
      )
      setSuccess('Account created! You can now sign in.')
      setRegisterForm(REGISTER_INIT)
      switchTab('login')
      setLoginForm((f) => ({ ...f, email: registerForm.email }))
    } catch (err) {
      setError(err.response?.data?.message ?? 'Registration failed. Try a different email.')
    } finally {
      setLoading(false)
    }
  }

  // ── UI ───────────────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen flex">

      {/* ── Left brand panel ── */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-blue-700 to-blue-900 flex-col justify-between p-12">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 bg-white/20 rounded-lg flex items-center justify-center">
            <HeartIcon className="w-5 h-5 text-white" />
          </div>
          <span className="text-white font-semibold text-lg">HeartSync</span>
        </div>

        <div>
          <h1 className="text-4xl font-bold text-white leading-tight mb-4">
            Multimodal Cardiac<br />Decision Support
          </h1>
          <p className="text-blue-200 text-base leading-relaxed max-w-sm">
            ECG analysis and AI-powered coronary segmentation for Cath Lab clinical workflows.
          </p>
          <div className="mt-8 space-y-3">
            {['ECG signal processing', 'AI coronary segmentation', 'Automated PDF reports'].map((f) => (
              <div key={f} className="flex items-center gap-3 text-blue-100 text-sm">
                <div className="w-5 h-5 rounded-full bg-blue-500/50 flex items-center justify-center flex-shrink-0">
                  <CheckIcon className="w-3 h-3 text-white" />
                </div>
                {f}
              </div>
            ))}
          </div>
        </div>

        <p className="text-blue-300 text-xs">CO4353 Distributed Systems — Semester 8</p>
      </div>

      {/* ── Right form panel ── */}
      <div className="flex-1 flex items-center justify-center p-8 bg-white">
        <div className="w-full max-w-sm">

          {/* Mobile logo */}
          <div className="lg:hidden flex items-center gap-2 mb-8">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
              <HeartIcon className="w-5 h-5 text-white" />
            </div>
            <span className="font-semibold text-slate-800">HeartSync</span>
          </div>

          {/* Tab switcher */}
          <div className="flex rounded-lg border border-slate-200 p-1 mb-7 bg-slate-50">
            <TabBtn active={tab === 'login'}    onClick={() => switchTab('login')}>Sign in</TabBtn>
            <TabBtn active={tab === 'register'} onClick={() => switchTab('register')}>Register</TabBtn>
          </div>

          {/* Feedback banners */}
          {error && (
            <div className="mb-5 px-4 py-3 rounded-lg bg-red-50 border border-red-200 text-sm text-red-700">
              {error}
            </div>
          )}
          {success && (
            <div className="mb-5 px-4 py-3 rounded-lg bg-green-50 border border-green-200 text-sm text-green-700">
              {success}
            </div>
          )}

          {/* ── Login form ── */}
          {tab === 'login' && (
            <>
              <h2 className="text-2xl font-bold text-slate-800 mb-1">Welcome back</h2>
              <p className="text-sm text-slate-500 mb-7">Enter your credentials to access the platform.</p>

              <form onSubmit={handleLogin} className="space-y-5">
                <Field label="Email address">
                  <input
                    name="email" type="email" autoComplete="email" required
                    value={loginForm.email} onChange={changeLogin}
                    className="input-field" placeholder="you@hospital.org"
                  />
                </Field>
                <Field label="Password">
                  <input
                    name="password" type="password" autoComplete="current-password" required
                    value={loginForm.password} onChange={changeLogin}
                    className="input-field" placeholder="••••••••"
                  />
                </Field>
                <SubmitBtn loading={loading} label="Sign in" loadingLabel="Signing in…" />
              </form>

              <p className="mt-6 text-center text-sm text-slate-500">
                No account?{' '}
                <button onClick={() => switchTab('register')} className="text-blue-600 font-medium hover:underline">
                  Register here
                </button>
              </p>
            </>
          )}

          {/* ── Register form ── */}
          {tab === 'register' && (
            <>
              <h2 className="text-2xl font-bold text-slate-800 mb-1">Create account</h2>
              <p className="text-sm text-slate-500 mb-7">Register your clinical credentials.</p>

              <form onSubmit={handleRegister} className="space-y-4">
                <Field label="Full name">
                  <input
                    name="name" type="text" autoComplete="name" required
                    value={registerForm.name} onChange={changeRegister}
                    className="input-field" placeholder="Dr. Jane Smith"
                  />
                </Field>
                <Field label="Email address">
                  <input
                    name="email" type="email" autoComplete="email" required
                    value={registerForm.email} onChange={changeRegister}
                    className="input-field" placeholder="you@hospital.org"
                  />
                </Field>
                <Field label="Role">
                  <select
                    name="role"
                    value={registerForm.role} onChange={changeRegister}
                    className="input-field"
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>{r.charAt(0) + r.slice(1).toLowerCase()}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Password">
                  <input
                    name="password" type="password" autoComplete="new-password" required minLength={6}
                    value={registerForm.password} onChange={changeRegister}
                    className="input-field" placeholder="Min. 6 characters"
                  />
                </Field>
                <Field label="Confirm password">
                  <input
                    name="confirmPassword" type="password" autoComplete="new-password" required
                    value={registerForm.confirmPassword} onChange={changeRegister}
                    className="input-field" placeholder="••••••••"
                  />
                </Field>
                <div className="pt-1">
                  <SubmitBtn loading={loading} label="Create account" loadingLabel="Creating…" />
                </div>
              </form>

              <p className="mt-6 text-center text-sm text-slate-500">
                Already have an account?{' '}
                <button onClick={() => switchTab('login')} className="text-blue-600 font-medium hover:underline">
                  Sign in
                </button>
              </p>
            </>
          )}

        </div>
      </div>
    </div>
  )
}

// ── small helpers ────────────────────────────────────────────────────────────

function TabBtn({ active, onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex-1 py-1.5 text-sm font-medium rounded-md transition-colors ${
        active ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'
      }`}
    >
      {children}
    </button>
  )
}

function Field({ label, children }) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1.5">{label}</label>
      {children}
    </div>
  )
}

function SubmitBtn({ loading, label, loadingLabel }) {
  return (
    <button type="submit" disabled={loading} className="btn-primary w-full justify-center py-2.5">
      {loading ? (
        <>
          <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
          {loadingLabel}
        </>
      ) : label}
    </button>
  )
}

function HeartIcon({ className }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
    </svg>
  )
}

function CheckIcon({ className }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
    </svg>
  )
}
