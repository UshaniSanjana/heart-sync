import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import {
  getPatient, getEcgRecords, uploadEcg, analyzeAngiogram,
  getAiResult, getReports, downloadReport, generateReport,
  deleteEcg, deleteReport,
} from '../api/client.js'

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtDate(str) {
  if (!str) return '—'
  try {
    return new Date(str).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
  } catch { return str.slice(0, 10) }
}

function fmtTime(str) {
  if (!str) return ''
  try { return new Date(str).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }) }
  catch { return '' }
}

// Doctor-friendly status labels
const STATUS_LABEL = {
  PENDING:    'Awaiting Analysis',
  PROCESSING: 'Analysing…',
  ANALYZED:   'Analysis Complete',
  COMPLETED:  'Ready',
  FAILED:     'Failed',
}

// ── Badge tokens ─────────────────────────────────────────────────────────────

const RISK_STYLE = {
  HIGH:     { cls: 'bg-red-50 text-red-600 ring-1 ring-red-200',     dot: 'bg-red-400',     label: 'High Risk' },
  MODERATE: { cls: 'bg-amber-50 text-amber-600 ring-1 ring-amber-200', dot: 'bg-amber-400', label: 'Moderate Risk' },
  LOW:      { cls: 'bg-emerald-50 text-emerald-600 ring-1 ring-emerald-200', dot: 'bg-emerald-400', label: 'Low Risk' },
}
const STATUS_STYLE = {
  PENDING:    'bg-slate-100 text-slate-500',
  PROCESSING: 'bg-blue-50 text-blue-600',
  ANALYZED:   'bg-teal-50 text-teal-600',
  COMPLETED:  'bg-emerald-50 text-emerald-600',
  FAILED:     'bg-red-50 text-red-500',
}
const SEVERITY_STYLE = {
  CRITICAL: 'bg-red-50 text-red-600',
  HIGH:     'bg-orange-50 text-orange-600',
  MODERATE: 'bg-amber-50 text-amber-600',
  LOW:      'bg-emerald-50 text-emerald-600',
}
const SEVERITY_LABEL = {
  CRITICAL: 'Critical', HIGH: 'Significant', MODERATE: 'Moderate', LOW: 'Mild',
}

// ── Small atoms ───────────────────────────────────────────────────────────────

function RiskBadge({ risk }) {
  const s = RISK_STYLE[risk]
  if (!s) return <span className="badge bg-slate-100 text-slate-500">Unknown</span>
  return (
    <span className={`badge ${s.cls}`}>
      <span className={`w-1.5 h-1.5 rounded-full mr-1.5 inline-block ${s.dot}`} />
      {s.label}
    </span>
  )
}

function StatusBadge({ status }) {
  return (
    <span className={`badge ${STATUS_STYLE[status] ?? 'bg-slate-100 text-slate-500'}`}>
      {STATUS_LABEL[status] ?? status}
    </span>
  )
}

function Spinner({ size = 'md' }) {
  const s = size === 'sm' ? 'w-4 h-4 border-2' : 'w-5 h-5 border-[2.5px]'
  return <span className={`${s} border-blue-500 border-t-transparent rounded-full animate-spin inline-block flex-shrink-0`} />
}

// Section card
function Card({ title, subtitle, icon, iconBg = 'bg-slate-100', iconColor = 'text-slate-500', action, children }) {
  return (
    <div className="glass-card overflow-hidden">
      <div className="px-5 py-4 flex items-center gap-3 border-b border-slate-100/60">
        {icon && (
          <span className={`w-8 h-8 rounded-xl ${iconBg} ${iconColor} flex items-center justify-center flex-shrink-0`}>
            {icon}
          </span>
        )}
        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-semibold text-slate-800">{title}</h3>
          {subtitle && <p className="text-xs text-slate-400 mt-0.5 truncate">{subtitle}</p>}
        </div>
        {action}
      </div>
      <div className="p-5">{children}</div>
    </div>
  )
}

// Metric row
function MetricRow({ label, value }) {
  return (
    <div className="glass-row flex items-center justify-between py-2.5 px-0.5">
      <span className="text-xs text-slate-400 font-medium">{label}</span>
      <span className="text-sm font-semibold text-slate-800">{value ?? <span className="text-slate-300">—</span>}</span>
    </div>
  )
}

// Empty state
function Empty({ icon, text, hint }) {
  return (
    <div className="text-center py-10">
      <span className="w-11 h-11 rounded-2xl glass-inner flex items-center justify-center mx-auto mb-3 text-slate-300">{icon}</span>
      <p className="text-sm font-medium text-slate-500">{text}</p>
      {hint && <p className="text-xs text-slate-400 mt-1">{hint}</p>}
    </div>
  )
}

// ── SVG icon constants ────────────────────────────────────────────────────────

const IcnEcg     = <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M3 12h2l2-8 4 16 3-10 2 6 2-4h3" /></svg>
const IcnMicro   = <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" /></svg>
const IcnHeart   = <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" /></svg>
const IcnDoc     = <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>
const IcnResults = <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" /></svg>
const IcnDl      = <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}><path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
const IcnUp      = <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" /></svg>

// ── ECG Interpretation sub-card ───────────────────────────────────────────────

function EcgInterpretation({ ecgRecordId }) {
  const [result, setResult]   = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!ecgRecordId) return
    setLoading(true)
    getAiResult(ecgRecordId)
      .then(({ data }) => setResult(data))
      .catch(() => setResult(null))
      .finally(() => setLoading(false))
  }, [ecgRecordId])

  if (loading) return (
    <Card title="Automated ECG Interpretation" icon={IcnMicro} iconBg="bg-violet-50" iconColor="text-violet-500">
      <div className="flex justify-center py-8"><Spinner /></div>
    </Card>
  )

  if (!result) return (
    <Card title="Automated ECG Interpretation" icon={IcnMicro} iconBg="bg-violet-50" iconColor="text-violet-500">
      <Empty
        icon={IcnMicro}
        text="Interpretation not yet available"
        hint="The recording is still being reviewed. Please check back shortly."
      />
    </Card>
  )

  return (
    <Card title="Automated ECG Interpretation" icon={IcnMicro} iconBg="bg-violet-50" iconColor="text-violet-500">
      <div className="space-y-3">

        {/* Risk + Certainty */}
        <div className="glass-inner flex items-center justify-between px-4 py-3.5">
          <div>
            <p className="text-[10px] text-slate-400 uppercase tracking-widest font-semibold mb-1.5">Cardiac Risk</p>
            <RiskBadge risk={result.overallRisk} />
          </div>
          {result.confidence != null && (
            <div className="text-right">
              <p className="text-[10px] text-slate-400 uppercase tracking-widest font-semibold mb-1">Certainty</p>
              <p className="text-2xl font-black text-slate-800 tabular-nums leading-none">
                {(result.confidence * 100).toFixed(0)}<span className="text-xs font-semibold text-slate-400 ml-0.5">%</span>
              </p>
            </div>
          )}
        </div>

        {/* Diagnosis */}
        {result.predictionLabel && (
          <div className="px-4 py-3 rounded-xl bg-blue-50/60 border border-blue-100/80">
            <p className="text-[10px] text-blue-400 uppercase tracking-widest font-semibold mb-1">Suggested Diagnosis</p>
            <p className="text-sm font-bold text-slate-800">{result.predictionLabel}</p>
          </div>
        )}

        {/* Clinical findings table */}
        {result.coronaryFindings && Object.keys(result.coronaryFindings).length > 0 && (
          <div>
            <p className="text-[10px] text-slate-400 uppercase tracking-widest font-semibold mb-2">Clinical Findings</p>
            <div className="glass-inner overflow-hidden divide-y divide-slate-100/60">
              {Object.entries(result.coronaryFindings).map(([k, v]) => (
                <div key={k} className="flex items-center justify-between px-4 py-2.5 hover:bg-slate-50/60 transition-colors">
                  <span className="text-xs text-slate-500">{k}</span>
                  <span className="text-xs font-semibold text-slate-700">{v}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        <p className="text-[10px] text-slate-300 pt-1">AI-assisted review — always confirm findings clinically before making decisions.</p>
      </div>
    </Card>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function PatientPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const fileRef  = useRef()
  const angioRef = useRef()

  const [patient,      setPatient]      = useState(null)
  const [ecgList,      setEcgList]      = useState([])
  const [reports,      setReports]      = useState([])
  const [selectedEcg,  setSelectedEcg]  = useState(null)

  const [loadingPatient, setLoadingPatient] = useState(true)
  const [loadingEcg,     setLoadingEcg]     = useState(true)
  const [loadingReports, setLoadingReports] = useState(true)
  const [uploading,      setUploading]      = useState(false)
  const [downloadingId,  setDownloadingId]  = useState(null)
  const [generatingReport, setGeneratingReport] = useState(false)

  const [uploadError,    setUploadError]    = useState('')
  const [angioResult,    setAngioResult]    = useState(null)
  const [analyzingAngio, setAnalyzingAngio] = useState(false)
  const [angioError,     setAngioError]     = useState('')
  const [generateError,  setGenerateError]  = useState('')

  // Delete state — tracks which item is pending confirm and which is mid-delete
  const [confirmDeleteEcg,    setConfirmDeleteEcg]    = useState(null) // ecg id
  const [deletingEcgId,       setDeletingEcgId]       = useState(null)
  const [confirmDeleteReport,  setConfirmDeleteReport]  = useState(null) // report id
  const [deletingReportId,     setDeletingReportId]     = useState(null)

  useEffect(() => {
    Promise.all([
      getPatient(id).then(({ data }) => setPatient(data)).finally(() => setLoadingPatient(false)),
      getEcgRecords(id).then(({ data }) => {
        setEcgList(data)
        if (data.length > 0) setSelectedEcg(data[0])
      }).finally(() => setLoadingEcg(false)),
      getReports(id).then(({ data }) => setReports(data)).finally(() => setLoadingReports(false)),
    ])
  }, [id])

  const handleUpload = async (e) => {
    const file = e.target.files?.[0]; if (!file) return
    setUploading(true); setUploadError('')
    try {
      const { data } = await uploadEcg(id, file)
      setEcgList(prev => [data, ...prev])
      setSelectedEcg(data)
    } catch (err) {
      setUploadError(err.response?.data?.message ?? 'Upload failed. Please try again.')
    } finally { setUploading(false); fileRef.current.value = '' }
  }

  const handleAngioUpload = async (e) => {
    const file = e.target.files?.[0]; if (!file) return
    setAnalyzingAngio(true); setAngioError(''); setAngioResult(null)
    try {
      const { data } = await analyzeAngiogram(id, file)
      setAngioResult(data)
    } catch (err) {
      setAngioError(err.response?.data?.detail ?? err.response?.data?.message ?? 'Analysis could not be completed. Please try again.')
    } finally { setAnalyzingAngio(false); angioRef.current.value = '' }
  }

  const handleGenerate = async () => {
    setGeneratingReport(true); setGenerateError('')
    try {
      await generateReport(id, selectedEcg?.id)
      const { data } = await getReports(id)
      setReports(data)
    } catch (err) {
      setGenerateError(err.response?.data?.message ?? 'Report could not be generated. Please try again.')
    } finally { setGeneratingReport(false) }
  }

  const handleDownload = async (report) => {
    setDownloadingId(report.id)
    try {
      const date = fmtDate(report.generatedAt ?? report.createdAt).replace(/ /g, '-')
      await downloadReport(report.id, `Cardiac-Assessment-${patient?.lastName ?? id}-${date}.pdf`)
    } catch { /* ignore */ }
    finally { setDownloadingId(null) }
  }

  const handleDeleteEcg = async (ecgId) => {
    setDeletingEcgId(ecgId)
    try {
      await deleteEcg(ecgId)
      setEcgList(prev => prev.filter(e => e.id !== ecgId))
      if (selectedEcg?.id === ecgId) {
        const remaining = ecgList.filter(e => e.id !== ecgId)
        setSelectedEcg(remaining.length > 0 ? remaining[0] : null)
      }
    } catch { /* silently ignore — row stays */ }
    finally { setDeletingEcgId(null); setConfirmDeleteEcg(null) }
  }

  const handleDeleteReport = async (reportId) => {
    setDeletingReportId(reportId)
    try {
      await deleteReport(reportId)
      setReports(prev => prev.filter(r => r.id !== reportId))
    } catch { /* silently ignore — row stays */ }
    finally { setDeletingReportId(null); setConfirmDeleteReport(null) }
  }

  // ── Loading / 404 ─────────────────────────────────────────────────────────

  if (loadingPatient) return (
    <Shell><div className="flex justify-center py-32"><Spinner /></div></Shell>
  )
  if (!patient) return (
    <Shell><p className="text-center text-slate-400 text-sm py-20">Patient record not found.</p></Shell>
  )

  const initials = `${patient.firstName?.[0] ?? ''}${patient.lastName?.[0] ?? ''}`

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <Shell>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-400">
          <button onClick={() => navigate('/dashboard')} className="hover:text-slate-700 transition-colors font-medium">
            Patient Registry
          </button>
          <svg className="w-3 h-3 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" /></svg>
          <span className="text-slate-600 font-medium">{patient.firstName} {patient.lastName}</span>
        </nav>

        {/* Patient header */}
        <div className="glass-card px-6 py-5">
          <div className="flex items-start gap-5">

            {/* Avatar */}
            <div className="relative flex-shrink-0">
              <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white text-lg font-bold shadow-lg shadow-blue-200/50">
                {initials}
              </div>
              <span className="absolute -bottom-1 -right-1 w-4 h-4 bg-emerald-400 rounded-full border-2 border-white shadow-sm" title="Active" />
            </div>

            {/* Name + demographics */}
            <div className="flex-1 min-w-0">
              <h1 className="text-xl font-bold text-slate-900">{patient.firstName} {patient.lastName}</h1>
              <p className="text-xs text-slate-400 mt-0.5">Patient ID: {patient.id?.slice(0, 8).toUpperCase()}</p>

              <div className="flex flex-wrap gap-2 mt-3">
                {[
                  patient.dateOfBirth  && { label: `DOB: ${fmtDate(patient.dateOfBirth)}` },
                  patient.gender       && { label: patient.gender === 'FEMALE' ? 'Female' : patient.gender === 'MALE' ? 'Male' : patient.gender },
                  patient.contactNumber && { label: patient.contactNumber },
                  patient.bloodType    && { label: `Blood Type: ${patient.bloodType}` },
                ].filter(Boolean).map(({ label }) => (
                  <span key={label} className="inline-flex items-center px-2.5 py-1 rounded-lg bg-slate-100/70 text-slate-600 text-xs font-medium border border-slate-200/50">
                    {label}
                  </span>
                ))}
              </div>
            </div>

            {/* Summary counts */}
            <div className="hidden lg:flex items-center gap-3">
              {[
                { label: 'ECG Records',   value: ecgList.length,   color: 'text-blue-600',    bg: 'bg-blue-50',    loading: loadingEcg },
                { label: 'Reports',       value: reports.length,   color: 'text-emerald-600', bg: 'bg-emerald-50', loading: loadingReports },
              ].map(({ label, value, color, bg, loading: l }) => (
                <div key={label} className={`text-center px-4 py-2.5 rounded-xl ${bg} border border-white/80`}>
                  <p className={`text-xl font-black ${color} tabular-nums leading-none`}>{l ? '…' : value}</p>
                  <p className="text-[10px] text-slate-400 mt-1 font-medium">{label}</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Two-column workspace */}
        <div className="grid lg:grid-cols-2 gap-6">

          {/* ── LEFT: ECG column ───────────────────────────────────────── */}
          <div className="space-y-5">

            {/* ECG recordings list */}
            <Card
              title="ECG Recordings"
              subtitle={ecgList.length > 0 ? `${ecgList.length} recording${ecgList.length !== 1 ? 's' : ''} on file` : 'No recordings on file'}
              icon={IcnEcg}
              iconBg="bg-blue-50"
              iconColor="text-blue-500"
            >
              {/* Upload */}
              <input ref={fileRef} type="file" accept=".csv,.txt,.xml,.dcm,.png,.jpg,.jpeg,.bmp,.tiff,.tif" className="hidden" onChange={handleUpload} />
              <button onClick={() => fileRef.current?.click()} disabled={uploading} className="btn-primary w-full justify-center mb-4">
                {uploading ? <><Spinner size="sm" />Uploading…</> : <>{IcnUp}Add ECG Recording</>}
              </button>
              {uploadError && (
                <div className="mb-3 flex items-center gap-2 px-3 py-2.5 rounded-xl bg-red-50 border border-red-100 text-xs text-red-600">
                  <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" /></svg>
                  {uploadError}
                </div>
              )}

              {loadingEcg
                ? <div className="flex justify-center py-6"><Spinner /></div>
                : ecgList.length === 0
                  ? <Empty
                      icon={IcnEcg}
                      text="No ECG recordings on file"
                      hint="Add a recording above to begin automated analysis."
                    />
                  : <div className="space-y-1.5">
                      {ecgList.map((ecg, i) => {
                        const isConfirming = confirmDeleteEcg === ecg.id
                        const isDeleting   = deletingEcgId    === ecg.id
                        return (
                          <div
                            key={ecg.id}
                            className={`rounded-xl border transition-all duration-150 ${
                              isConfirming
                                ? 'bg-red-50/70 border-red-200/70'
                                : selectedEcg?.id === ecg.id
                                  ? 'bg-blue-50/80 border-blue-200/80 ring-1 ring-blue-200/60'
                                  : 'bg-white/60 border-slate-200/50 hover:bg-slate-50/70 hover:border-slate-200'
                            }`}
                          >
                            {isConfirming ? (
                              /* ── Confirm delete row ── */
                              <div className="px-4 py-3 flex items-center justify-between gap-3">
                                <p className="text-xs font-semibold text-red-600">Remove ECG Recording {ecgList.length - i}?</p>
                                <div className="flex items-center gap-2 flex-shrink-0">
                                  <button
                                    onClick={() => setConfirmDeleteEcg(null)}
                                    className="text-xs px-2.5 py-1 rounded-lg bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 transition-colors"
                                  >
                                    Cancel
                                  </button>
                                  <button
                                    onClick={() => handleDeleteEcg(ecg.id)}
                                    disabled={isDeleting}
                                    className="text-xs px-2.5 py-1 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors flex items-center gap-1.5 disabled:opacity-50"
                                  >
                                    {isDeleting ? <Spinner size="sm" /> : null}
                                    Remove
                                  </button>
                                </div>
                              </div>
                            ) : (
                              /* ── Normal row ── */
                              <div className="flex items-center gap-2 pr-2">
                                <button
                                  onClick={() => setSelectedEcg(ecg)}
                                  className="flex-1 text-left px-4 py-3 min-w-0"
                                >
                                  <div className="flex items-center justify-between gap-2">
                                    <div className="flex items-center gap-2.5 min-w-0">
                                      <span className={`w-2 h-2 rounded-full flex-shrink-0 ${selectedEcg?.id === ecg.id ? 'bg-blue-400' : 'bg-slate-300'}`} />
                                      <span className="text-sm font-semibold text-slate-700">
                                        ECG Recording {ecgList.length - i}
                                      </span>
                                    </div>
                                    <StatusBadge status={ecg.status} />
                                  </div>
                                  <p className="text-xs text-slate-400 mt-1 pl-4">
                                    {fmtDate(ecg.uploadedAt)} {fmtTime(ecg.uploadedAt) && `· ${fmtTime(ecg.uploadedAt)}`}
                                    <span className="text-slate-300 ml-1.5 font-mono text-[10px]">{ecg.fileName}</span>
                                  </p>
                                </button>
                                <button
                                  onClick={(e) => { e.stopPropagation(); setConfirmDeleteEcg(ecg.id) }}
                                  className="w-7 h-7 flex items-center justify-center rounded-lg text-slate-300 hover:text-red-400 hover:bg-red-50 transition-colors flex-shrink-0"
                                  title="Remove recording"
                                >
                                  <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                                  </svg>
                                </button>
                              </div>
                            )}
                          </div>
                        )
                      })}
                    </div>
              }
            </Card>

            {/* ECG measurements */}
            {selectedEcg && (
              <Card
                title="ECG Measurements"
                subtitle={`Recording ${ecgList.length - ecgList.findIndex(e => e.id === selectedEcg.id)} · ${fmtDate(selectedEcg.uploadedAt)}`}
                icon={IcnResults}
                iconBg="bg-teal-50"
                iconColor="text-teal-500"
              >
                <div className="flex items-center justify-between py-2 border-b border-slate-100/60 mb-1">
                  <span className="text-xs text-slate-400 font-medium">Review Status</span>
                  <StatusBadge status={selectedEcg.status} />
                </div>
                {selectedEcg.heartRate || selectedEcg.rhythm
                  ? <>
                      <MetricRow label="Heart Rate"    value={selectedEcg.heartRate ? `${selectedEcg.heartRate} bpm` : null} />
                      <MetricRow label="Rhythm"        value={selectedEcg.rhythm} />
                      <MetricRow label="PR Interval"   value={selectedEcg.prInterval  ? `${selectedEcg.prInterval} ms`  : null} />
                      <MetricRow label="QRS Duration"  value={selectedEcg.qrsDuration ? `${selectedEcg.qrsDuration} ms` : null} />
                      <MetricRow label="QT Interval"   value={selectedEcg.qtInterval  ? `${selectedEcg.qtInterval} ms`  : null} />
                      {selectedEcg.findings && (
                        <div className="mt-3 pt-3 border-t border-slate-100/60">
                          <p className="text-[10px] text-slate-400 uppercase tracking-widest font-semibold mb-1.5">Reported Findings</p>
                          <p className="text-xs text-slate-700 glass-inner px-3 py-2.5 leading-relaxed">{selectedEcg.findings}</p>
                        </div>
                      )}
                    </>
                  : (
                    <div className="py-5 text-center">
                      <p className="text-sm text-slate-400">Measurements not yet available</p>
                      <p className="text-xs text-slate-300 mt-1">Analysis is in progress — results will appear here shortly.</p>
                    </div>
                  )
                }
              </Card>
            )}

            {/* AI interpretation */}
            {selectedEcg && <EcgInterpretation ecgRecordId={selectedEcg.id} />}
          </div>

          {/* ── RIGHT: Angiogram + Reports ─────────────────────────────── */}
          <div className="space-y-5">

            {/* Coronary angiogram */}
            <Card title="Coronary Angiogram" icon={IcnHeart} iconBg="bg-rose-50" iconColor="text-rose-500">
              {/* Upload */}
              <input ref={angioRef} type="file" accept=".dcm,.png,.jpg,.jpeg,.tiff,.tif" className="hidden" onChange={handleAngioUpload} />
              <button onClick={() => angioRef.current?.click()} disabled={analyzingAngio} className="btn-secondary w-full justify-center mb-1">
                {analyzingAngio ? <><Spinner size="sm" />Analysing image…</> : <>{IcnUp}Upload Coronary Angiogram</>}
              </button>
              <p className="text-xs text-slate-400 mb-4">Accepts DICOM (.dcm) or image files (.png, .jpg)</p>

              {angioError && (
                <div className="mb-3 flex items-center gap-2 px-3 py-2.5 rounded-xl bg-red-50 border border-red-100 text-xs text-red-600">
                  <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" /></svg>
                  {angioError}
                </div>
              )}

              {!angioResult && !analyzingAngio && (
                <Empty
                  icon={IcnHeart}
                  text="No angiogram on file"
                  hint="Upload a coronary angiogram image to begin vessel analysis."
                />
              )}

              {angioResult && (
                <div className="space-y-4">
                  {/* Summary tiles */}
                  <div className="grid grid-cols-3 gap-2.5">
                    {[
                      { label: 'Risk Level',       node: <RiskBadge risk={angioResult.overallRisk} /> },
                      { label: 'Vessels Found',    node: <span className="text-xl font-black text-slate-800">{angioResult.totalBranches}</span> },
                      { label: 'Result Certainty', node: <span className="text-xl font-black text-slate-800">{(angioResult.confidence * 100).toFixed(0)}<span className="text-[11px] font-semibold text-slate-400">%</span></span> },
                    ].map(({ label, node }) => (
                      <div key={label} className="glass-inner px-3 py-3 text-center">
                        <p className="text-[9px] text-slate-400 uppercase tracking-widest font-semibold mb-2">{label}</p>
                        {node}
                      </div>
                    ))}
                  </div>

                  {/* Vessel map */}
                  {angioResult.overlayBase64 && (
                    <div>
                      <p className="text-[10px] text-slate-400 uppercase tracking-widest font-semibold mb-2">Vessel Map</p>
                      <img
                        src={`data:image/png;base64,${angioResult.overlayBase64}`}
                        alt="Coronary vessel map"
                        className="w-full rounded-xl border border-slate-200/60"
                        style={{ boxShadow: '0 2px 12px rgba(0,0,0,0.06)' }}
                      />
                    </div>
                  )}

                  {/* Stenosis findings table */}
                  {angioResult.lesions?.length > 0 ? (
                    <div>
                      <p className="text-[10px] text-slate-400 uppercase tracking-widest font-semibold mb-2">
                        Stenotic Lesions — {angioResult.lesions.length} found
                      </p>
                      <div className="glass-inner overflow-hidden">
                        <table className="w-full text-xs">
                          <thead>
                            <tr className="border-b border-slate-200/50 bg-slate-50/50">
                              {['No.', 'Grade', 'Stenosis', 'Min. Lumen', 'Ref. Vessel', 'Length'].map(h => (
                                <th key={h} className="text-left px-3 py-2.5 text-[10px] font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">{h}</th>
                              ))}
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-100/60">
                            {angioResult.lesions.map(l => (
                              <tr key={l.rank} className="hover:bg-slate-50/50 transition-colors">
                                <td className="px-3 py-2.5 text-slate-500 font-bold">{l.rank}</td>
                                <td className="px-3 py-2.5">
                                  <span className={`badge ${SEVERITY_STYLE[l.severity?.toUpperCase()] ?? 'bg-slate-100 text-slate-500'}`}>
                                    {SEVERITY_LABEL[l.severity?.toUpperCase()] ?? l.severity}
                                  </span>
                                </td>
                                <td className="px-3 py-2.5 font-bold text-slate-800">{l.dsPercent.toFixed(1)}%</td>
                                <td className="px-3 py-2.5 text-slate-500">{l.mldPx.toFixed(1)}</td>
                                <td className="px-3 py-2.5 text-slate-500">{l.rvdPx.toFixed(1)}</td>
                                <td className="px-3 py-2.5 text-slate-500">{l.lengthPx.toFixed(1)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      <p className="text-[10px] text-slate-300 mt-2">Measurements are relative vessel dimensions from the imaging data.</p>
                    </div>
                  ) : (
                    <div className="glass-inner px-4 py-3 flex items-center gap-2">
                      <span className="w-2 h-2 rounded-full bg-emerald-400 flex-shrink-0" />
                      <p className="text-sm text-slate-600 font-medium">No significant stenotic lesions detected.</p>
                    </div>
                  )}
                </div>
              )}
            </Card>

            {/* Patient reports */}
            <Card
              title="Patient Reports"
              subtitle="Comprehensive cardiac assessment PDF reports"
              icon={IcnDoc}
              iconBg="bg-amber-50"
              iconColor="text-amber-500"
            >
              {/* Generate */}
              <div className="rounded-xl bg-slate-50/60 border border-slate-200/50 p-4 mb-5">
                <p className="text-xs font-semibold text-slate-700 mb-0.5">Generate New Report</p>
                <p className="text-xs text-slate-400 mb-3">
                  {selectedEcg && angioResult
                    ? 'Will include ECG measurements and coronary angiogram findings.'
                    : selectedEcg
                      ? 'Will include ECG measurements and interpretation.'
                      : angioResult
                        ? 'Will include coronary angiogram findings.'
                        : 'Select an ECG recording on the left to include ECG data.'}
                </p>
                <button onClick={handleGenerate} disabled={generatingReport} className="btn-primary w-full justify-center">
                  {generatingReport
                    ? <><Spinner size="sm" />Preparing report…</>
                    : <>{IcnDoc}Generate Cardiac Assessment Report</>}
                </button>
                {generateError && (
                  <p className="mt-2 text-xs text-red-600 flex items-center gap-1.5">
                    <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" /></svg>
                    {generateError}
                  </p>
                )}
              </div>

              {/* Reports list */}
              {loadingReports
                ? <div className="flex justify-center py-6"><Spinner /></div>
                : reports.length === 0
                  ? <Empty icon={IcnDoc} text="No reports on file" hint="Generate a report above to create the first assessment." />
                  : <div className="space-y-2">
                      {reports.map((r, i) => {
                        const dateStr  = r.generatedAt ?? r.createdAt
                        const isReady  = r.status === 'COMPLETED'
                        const isFailed = r.status === 'FAILED'
                        return (
                          <div
                            key={r.id}
                            className={`flex items-center gap-3 rounded-xl border px-4 py-3 transition-colors ${
                              isReady ? 'bg-white/70 border-slate-200/60 hover:bg-slate-50/60'
                              : isFailed ? 'bg-red-50/40 border-red-100/60'
                              : 'bg-slate-50/50 border-slate-200/40'
                            }`}
                          >
                            {/* Icon */}
                            <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 ${
                              isReady ? 'bg-emerald-50 text-emerald-500'
                              : isFailed ? 'bg-red-50 text-red-400'
                              : 'bg-slate-100 text-slate-400'
                            }`}>
                              {isReady
                                ? <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>
                                : isFailed
                                  ? <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" /></svg>
                                  : <Spinner size="sm" />
                              }
                            </div>

                            {/* Info */}
                            <div className="flex-1 min-w-0">
                              <p className="text-sm font-semibold text-slate-800">
                                Cardiac Assessment — {fmtDate(dateStr)}
                              </p>
                              <div className="flex items-center gap-2 mt-0.5">
                                <p className="text-xs text-slate-400">{fmtTime(dateStr)}</p>
                                {isFailed && <span className="text-xs text-red-500">Report could not be generated</span>}
                                {!isReady && !isFailed && <span className="text-xs text-slate-400">Being prepared…</span>}
                              </div>
                            </div>

                            {/* Download */}
                            {isReady && (
                              <button
                                onClick={() => handleDownload(r)}
                                disabled={downloadingId === r.id}
                                className="btn-secondary text-xs px-3 py-1.5 flex-shrink-0 gap-1.5"
                              >
                                {downloadingId === r.id ? <Spinner size="sm" /> : IcnDl}
                                Download
                              </button>
                            )}
                          </div>
                        )
                      })}
                    </div>
              }
            </Card>
          </div>
        </div>
      </div>
    </Shell>
  )
}

// ── Page shell ────────────────────────────────────────────────────────────────

function Shell({ children }) {
  return (
    <div className="min-h-screen relative">
      <div className="fixed inset-0 -z-10" style={{ background: 'linear-gradient(160deg,#ffffff 0%,#f8fafc 55%,#f0f5ff 100%)' }} />
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute -top-32 -right-32 w-[600px] h-[600px] bg-blue-100/20 rounded-full blur-3xl" />
        <div className="absolute bottom-0 -left-24 w-[500px] h-[500px] bg-indigo-100/15 rounded-full blur-3xl" />
      </div>
      <Navbar />
      {children}
    </div>
  )
}
