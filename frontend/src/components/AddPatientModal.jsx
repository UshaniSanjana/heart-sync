import { useState } from 'react'
import { createPatient } from '../api/client.js'

const INITIAL = { firstName: '', lastName: '', dateOfBirth: '', gender: 'MALE', contactNumber: '', email: '' }

export default function AddPatientModal({ onClose, onCreated }) {
  const [form, setForm] = useState(INITIAL)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const handleChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      const { data } = await createPatient(form)
      onCreated(data)
      onClose()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to create patient.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40">
      <div className="card w-full max-w-lg p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-base font-semibold text-slate-800">New Patient</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {error && (
          <div className="mb-4 px-3 py-2 rounded-lg bg-red-50 border border-red-200 text-sm text-red-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">First name</label>
              <input name="firstName" value={form.firstName} onChange={handleChange} required className="input-field" placeholder="John" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Last name</label>
              <input name="lastName" value={form.lastName} onChange={handleChange} required className="input-field" placeholder="Smith" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Date of birth</label>
              <input name="dateOfBirth" type="date" value={form.dateOfBirth} onChange={handleChange} required className="input-field" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Gender</label>
              <select name="gender" value={form.gender} onChange={handleChange} className="input-field">
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Contact number</label>
            <input name="contactNumber" value={form.contactNumber} onChange={handleChange} className="input-field" placeholder="+1 555 0100" />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Email</label>
            <input name="email" type="email" value={form.email} onChange={handleChange} className="input-field" placeholder="patient@example.com" />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
            <button type="submit" disabled={saving} className="btn-primary">
              {saving ? 'Saving…' : 'Add patient'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
