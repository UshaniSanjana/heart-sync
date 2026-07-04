import { useState } from 'react'
import { createPatient } from '../api/client.js'

const INITIAL = { firstName: '', lastName: '', dateOfBirth: '', gender: 'MALE', phone: '', email: '' }

export default function AddPatientModal({ onClose, onCreated }) {
  const [form, setForm]     = useState(INITIAL)
  const [saving, setSaving] = useState(false)
  const [error, setError]   = useState('')

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true); setError('')
    try {
      const { data } = await createPatient(form)
      onCreated(data); onClose()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to create patient.')
    } finally { setSaving(false) }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: 'rgba(15,23,42,0.55)', backdropFilter: 'blur(8px)' }}
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      {/* Glass modal */}
      <div
        className="w-full max-w-lg rounded-3xl border overflow-hidden"
        style={{
          background: 'rgba(255,255,255,0.75)',
          backdropFilter: 'blur(32px)',
          WebkitBackdropFilter: 'blur(32px)',
          borderColor: 'rgba(255,255,255,0.85)',
          boxShadow: '0 24px 64px rgba(100,116,139,0.25), inset 0 1px 0 rgba(255,255,255,0.95)',
        }}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b" style={{ borderColor: 'rgba(255,255,255,0.6)' }}>
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-md shadow-blue-300/40">
              <svg className="w-4.5 h-4.5 text-white w-[18px] h-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M19 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zM4 19.235v-.11a6.375 6.375 0 0112.75 0v.109A12.318 12.318 0 0110.374 21c-2.331 0-4.512-.645-6.374-1.766z" />
              </svg>
            </div>
            <div>
              <h2 className="text-base font-semibold text-slate-900">New Patient</h2>
              <p className="text-xs text-slate-400 mt-0.5">Add a patient to the registry</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-600 transition-colors"
            style={{ background: 'rgba(255,255,255,0.5)' }}
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Body */}
        <div className="px-6 py-5">
          {error && (
            <div
              className="mb-4 flex items-center gap-2 px-3.5 py-3 rounded-xl text-sm border"
              style={{ background: 'rgba(239,68,68,0.08)', borderColor: 'rgba(239,68,68,0.2)', color: '#dc2626' }}
            >
              <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
              </svg>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <ModalField label="First name">
                <ModalInput name="firstName" value={form.firstName} onChange={handleChange} required placeholder="Jane" />
              </ModalField>
              <ModalField label="Last name">
                <ModalInput name="lastName" value={form.lastName} onChange={handleChange} required placeholder="Smith" />
              </ModalField>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <ModalField label="Date of birth">
                <ModalInput name="dateOfBirth" type="date" value={form.dateOfBirth} onChange={handleChange} required />
              </ModalField>
              <ModalField label="Gender">
                <select name="gender" value={form.gender} onChange={handleChange}
                  className="block w-full px-3.5 py-2.5 text-sm text-slate-900 rounded-xl border transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-blue-400/40 focus:border-blue-300"
                  style={{ background: 'rgba(255,255,255,0.55)', backdropFilter: 'blur(8px)', borderColor: 'rgba(255,255,255,0.7)' }}>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </ModalField>
            </div>

            <ModalField label="Contact number">
              <ModalInput name="phone" value={form.phone} onChange={handleChange} placeholder="+1 555 0100" />
            </ModalField>

            <ModalField label="Email address">
              <ModalInput name="email" type="email" value={form.email} onChange={handleChange} placeholder="patient@example.com" />
            </ModalField>

            {/* Footer */}
            <div className="flex justify-end gap-3 pt-3 border-t mt-5" style={{ borderColor: 'rgba(255,255,255,0.5)' }}>
              <button
                type="button" onClick={onClose}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-600 rounded-xl border transition-all hover:text-slate-900"
                style={{ background: 'rgba(255,255,255,0.5)', backdropFilter: 'blur(8px)', borderColor: 'rgba(255,255,255,0.7)' }}
              >
                Cancel
              </button>
              <button
                type="submit" disabled={saving}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white rounded-xl transition-all shadow-lg disabled:opacity-50"
                style={{ background: 'linear-gradient(135deg, #3b82f6 0%, #4f46e5 100%)', boxShadow: '0 4px 14px rgba(79,70,229,0.28)' }}
              >
                {saving
                  ? <><span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />Saving…</>
                  : 'Add patient'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

function ModalField({ label, children }) {
  return (
    <div>
      <label className="block text-xs font-semibold text-slate-500 mb-1.5 uppercase tracking-wider">{label}</label>
      {children}
    </div>
  )
}

function ModalInput({ ...props }) {
  return (
    <input
      {...props}
      className="block w-full px-3.5 py-2.5 text-sm text-slate-900 rounded-xl border transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-blue-400/40 focus:border-blue-300 placeholder-slate-400"
      style={{ background: 'rgba(255,255,255,0.55)', backdropFilter: 'blur(8px)', borderColor: 'rgba(255,255,255,0.7)' }}
    />
  )
}
