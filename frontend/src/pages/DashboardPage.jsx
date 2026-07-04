import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import AddPatientModal from '../components/AddPatientModal.jsx'
import { getPatients, getEcgRecords, getReports, deletePatient } from '../api/client.js'

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtDate(str) {
  if (!str) return '—'
  try { return new Date(str).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) }
  catch { return str.slice(0, 10) }
}

// ── Stat cards ────────────────────────────────────────────────────────────────

const STAT_CONFIG = [
  {
    key: 'patients',
    label: 'Registered Patients',
    iconBg: 'bg-blue-50',
    iconColor: 'text-blue-500',
    icon: (
      <svg className="w-4.5 h-4.5 w-[18px] h-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
      </svg>
    ),
  },
  {
    key: 'ecg',
    label: 'ECG Recordings',
    iconBg: 'bg-violet-50',
    iconColor: 'text-violet-500',
    icon: (
      <svg className="w-[18px] h-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M3 12h2l2-8 4 16 3-10 2 6 2-4h3" />
      </svg>
    ),
  },
  {
    key: 'ai',
    label: 'Analyses Complete',
    iconBg: 'bg-teal-50',
    iconColor: 'text-teal-500',
    icon: (
      <svg className="w-[18px] h-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
      </svg>
    ),
  },
  {
    key: 'reports',
    label: 'Reports Generated',
    iconBg: 'bg-amber-50',
    iconColor: 'text-amber-500',
    icon: (
      <svg className="w-[18px] h-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
      </svg>
    ),
  },
]

function StatCard({ config, value }) {
  return (
    <div className="glass-card p-5 group hover:scale-[1.01] transition-transform duration-200">
      <div className="flex items-start justify-between mb-4">
        <div className={`w-10 h-10 rounded-xl ${config.iconBg} ${config.iconColor} flex items-center justify-center flex-shrink-0`}>
          {config.icon}
        </div>
      </div>
      <p className="text-2xl font-black text-slate-900 tabular-nums mb-0.5">
        {value != null
          ? value
          : <span className="inline-block w-5 h-5 rounded-full border-2 border-slate-200 border-t-slate-400 animate-spin" />}
      </p>
      <p className="text-xs text-slate-400 font-medium">{config.label}</p>
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function DashboardPage() {
  const [patients,  setPatients]  = useState([])
  const [loading,   setLoading]   = useState(true)
  const [search,    setSearch]    = useState('')
  const [showModal, setShowModal] = useState(false)
  const [stats,     setStats]     = useState({ ecg: null, reports: null })
  const [confirmDeletePatient, setConfirmDeletePatient] = useState(null)
  const [deletingPatientId, setDeletingPatientId] = useState(null)
  const navigate = useNavigate()

  useEffect(() => { fetchPatients() }, [])

  const fetchPatients = async () => {
    setLoading(true)
    try {
      const { data } = await getPatients()
      setPatients(data)
      fetchStats(data)
    } catch { /* handled by interceptor */ }
    finally { setLoading(false) }
  }

  const fetchStats = async (list) => {
    if (!list.length) { setStats({ ecg: 0, reports: 0 }); return }
    const [ecgCounts, reportCounts] = await Promise.all([
      Promise.all(list.map(p => getEcgRecords(p.id).then(r => r.data.length).catch(() => 0))),
      Promise.all(list.map(p => getReports(p.id).then(r => r.data.length).catch(() => 0))),
    ])
    setStats({
      ecg:     ecgCounts.reduce((a, b) => a + b, 0),
      reports: reportCounts.reduce((a, b) => a + b, 0),
    })
  }

  const handleDeletePatient = async (patientId) => {
    setDeletingPatientId(patientId)
    try {
      await deletePatient(patientId)
      const updated = patients.filter((p) => p.id !== patientId)
      setPatients(updated)
      fetchStats(updated)
    } catch { /* row stays if delete fails */ }
    finally {
      setDeletingPatientId(null)
      setConfirmDeletePatient(null)
    }
  }

  const statValues = {
    patients: patients.length,
    ecg:      stats.ecg,
    ai:       stats.ecg,
    reports:  stats.reports,
  }

  const filtered = patients.filter((p) => {
    const q = search.toLowerCase()
    const contact = p.phone ?? p.contactNumber ?? ''
    return (
      p.firstName?.toLowerCase().includes(q) ||
      p.lastName?.toLowerCase().includes(q)  ||
      contact.toLowerCase().includes(q)
    )
  })

  return (
    <div className="min-h-screen relative overflow-x-hidden">

      {/* Near-white gradient background — same as PatientPage */}
      <div className="fixed inset-0 -z-10" style={{ background: 'linear-gradient(160deg, #ffffff 0%, #f8fafc 55%, #f0f5ff 100%)' }} />
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute -top-32 -right-32 w-[600px] h-[600px] bg-blue-100/20 rounded-full blur-3xl" />
        <div className="absolute bottom-0 -left-24 w-[500px] h-[500px] bg-indigo-100/15 rounded-full blur-3xl" />
      </div>

      <Navbar />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

        {/* Page header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-slate-900">Patient Registry</h1>
            <p className="text-sm text-slate-400 mt-0.5">Manage patients and cardiac investigations</p>
          </div>
          <button onClick={() => setShowModal(true)} className="btn-primary">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            Add Patient
          </button>
        </div>

        {/* Stats row */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {STAT_CONFIG.map((cfg) => (
            <StatCard key={cfg.key} config={cfg} value={statValues[cfg.key]} />
          ))}
        </div>

        {/* Patient table */}
        <div className="glass-card overflow-hidden">

          {/* Toolbar */}
          <div className="px-5 py-4 border-b border-slate-100/60 flex items-center justify-between gap-4">
            <div className="relative flex-1 max-w-xs">
              <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
              </svg>
              <input
                type="text"
                placeholder="Search by name or contact…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="block w-full pl-9 pr-3.5 py-2 text-sm text-slate-900 rounded-xl border border-slate-200/60 bg-white/60 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-400/30 focus:border-blue-300/70 transition-all duration-150"
              />
            </div>
            <span className="text-xs text-slate-400 font-medium hidden sm:block">
              {filtered.length} {filtered.length === 1 ? 'patient' : 'patients'}
            </span>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-24">
              <div className="w-6 h-6 border-[2.5px] border-blue-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : filtered.length === 0 ? (
            <div className="text-center py-20">
              <div className="w-12 h-12 rounded-2xl glass-inner flex items-center justify-center mx-auto mb-3">
                <svg className="w-6 h-6 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
              </div>
              <p className="text-sm font-medium text-slate-500">
                {search ? 'No patients match your search' : 'No patients on record'}
              </p>
              <p className="text-xs text-slate-400 mt-1">
                {search ? 'Try a different name or contact number.' : 'Add your first patient to get started.'}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-slate-100/60 bg-slate-50/40">
                    <th className="text-left text-[11px] font-semibold text-slate-400 uppercase tracking-wider px-5 py-3">Patient</th>
                    <th className="text-left text-[11px] font-semibold text-slate-400 uppercase tracking-wider px-4 py-3 hidden sm:table-cell">Date of Birth</th>
                    <th className="text-left text-[11px] font-semibold text-slate-400 uppercase tracking-wider px-4 py-3 hidden md:table-cell">Gender</th>
                    <th className="text-left text-[11px] font-semibold text-slate-400 uppercase tracking-wider px-4 py-3 hidden lg:table-cell">Contact</th>
                    <th className="px-4 py-3 w-32" />
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((p) => {
                    const isConfirmingDelete = confirmDeletePatient === p.id
                    const isDeleting = deletingPatientId === p.id
                    return (
                      <tr
                        key={p.id}
                        onClick={() => navigate(`/patients/${p.id}`)}
                        className="border-b border-slate-100/50 cursor-pointer transition-colors duration-100 group"
                        onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(248,250,252,0.7)'}
                        onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                      >
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center text-white text-xs font-bold flex-shrink-0 shadow-sm ring-2 ring-white/80">
                            {p.firstName?.[0]}{p.lastName?.[0]}
                          </div>
                          <div>
                            <p className="text-sm font-semibold text-slate-800">{p.firstName} {p.lastName}</p>
                            <p className="text-xs text-slate-400 mt-0.5 sm:hidden">{fmtDate(p.dateOfBirth)}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3.5 text-sm text-slate-500 hidden sm:table-cell">{fmtDate(p.dateOfBirth)}</td>
                      <td className="px-4 py-3.5 hidden md:table-cell">
                        <span className={`badge text-[11px] ${
                          p.gender === 'FEMALE' ? 'bg-pink-50 text-pink-600 ring-1 ring-pink-200/60'
                          : p.gender === 'MALE'  ? 'bg-sky-50 text-sky-600 ring-1 ring-sky-200/60'
                          : 'bg-slate-100 text-slate-500'
                        }`}>
                          {p.gender === 'FEMALE' ? 'Female' : p.gender === 'MALE' ? 'Male' : p.gender}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 text-sm text-slate-500 hidden lg:table-cell">{p.phone || p.contactNumber || '—'}</td>
                        <td className="px-4 py-3.5 text-right">
                          {isConfirmingDelete ? (
                            <div
                              className="flex items-center justify-end gap-2"
                              onClick={(e) => e.stopPropagation()}
                            >
                              <button
                                onClick={() => setConfirmDeletePatient(null)}
                                className="text-xs px-2.5 py-1.5 rounded-lg bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 transition-colors"
                              >
                                Cancel
                              </button>
                              <button
                                onClick={() => handleDeletePatient(p.id)}
                                disabled={isDeleting}
                                className="text-xs px-2.5 py-1.5 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors flex items-center gap-1.5 disabled:opacity-50"
                              >
                                {isDeleting ? <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : null}
                                Delete
                              </button>
                            </div>
                          ) : (
                            <div className="flex items-center justify-end gap-2">
                              <button
                                onClick={(e) => {
                                  e.stopPropagation()
                                  setConfirmDeletePatient(p.id)
                                }}
                                disabled={isDeleting}
                                className="w-8 h-8 flex items-center justify-center rounded-lg text-slate-300 hover:text-red-400 hover:bg-red-50 transition-colors disabled:opacity-50"
                                title="Delete patient"
                              >
                                {isDeleting ? (
                                  <span className="w-3.5 h-3.5 border-2 border-red-200 border-t-red-500 rounded-full animate-spin" />
                                ) : (
                                  <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                                  </svg>
                                )}
                              </button>
                              <svg className="w-4 h-4 text-slate-300 group-hover:text-blue-400 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                                <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
                              </svg>
                            </div>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>

      {showModal && (
        <AddPatientModal
          onClose={() => setShowModal(false)}
          onCreated={(p) => setPatients((prev) => [p, ...prev])}
        />
      )}
    </div>
  )
}
