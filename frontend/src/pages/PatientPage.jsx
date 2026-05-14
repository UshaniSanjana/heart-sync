import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import {
  getPatient,
  getEcgRecords,
  uploadEcg,
  analyzeAngiogram,
  getReports,
  downloadReport,
} from '../api/client.js'

// ── helpers ─────────────────────────────────────────────────────────────────

function Section({ title, children }) {
  return (
    <div className="card">
      <div className="px-5 py-4 border-b border-slate-100">
        <h2 className="text-sm font-semibold text-slate-700">{title}</h2>
      </div>
      <div className="p-5">{children}</div>
    </div>
  )
}

function Field({ label, value }) {
  return (
    <div>
      <p className="text-xs text-slate-400 mb-0.5">{label}</p>
      <p className="text-sm font-medium text-slate-800">{value || '—'}</p>
    </div>
  )
}

function RiskBadge({ risk }) {
  const map = {
    HIGH:     'bg-red-100 text-red-700',
    MODERATE: 'bg-orange-100 text-orange-700',
    LOW:      'bg-green-100 text-green-700',
  }
  return <span className={`badge ${map[risk] ?? 'bg-slate-100 text-slate-600'}`}>{risk}</span>
}

function SeverityBadge({ severity }) {
  const map = {
    CRITICAL: 'bg-red-100 text-red-700',
    HIGH:     'bg-orange-100 text-orange-700',
    MODERATE: 'bg-yellow-100 text-yellow-700',
    LOW:      'bg-green-100 text-green-700',
  }
  return <span className={`badge ${map[severity?.toUpperCase()] ?? 'bg-slate-100 text-slate-600'}`}>{severity}</span>
}

function StatusBadge({ status }) {
  const map = {
    PENDING:    'bg-yellow-100 text-yellow-700',
    PROCESSING: 'bg-blue-100 text-blue-700',
    ANALYZED:   'bg-purple-100 text-purple-700',
    COMPLETED:  'bg-green-100 text-green-700',
    FAILED:     'bg-red-100 text-red-700',
  }
  return (
    <span className={`badge ${map[status] ?? 'bg-slate-100 text-slate-600'}`}>
      {status}
    </span>
  )
}

// ── main component ───────────────────────────────────────────────────────────

export default function PatientPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const fileRef   = useRef()
  const angioRef  = useRef()

  const [patient, setPatient]   = useState(null)
  const [ecgList, setEcgList]   = useState([])
  const [reports, setReports]   = useState([])
  const [selectedEcg, setSelectedEcg] = useState(null)

  const [loadingPatient, setLoadingPatient] = useState(true)
  const [loadingEcg, setLoadingEcg]         = useState(true)
  const [uploading, setUploading]           = useState(false)
  const [loadingReports, setLoadingReports] = useState(true)
  const [downloadingId, setDownloadingId]   = useState(null)

  const [uploadError, setUploadError] = useState('')

  // angiogram analysis state
  const [angioResult,   setAngioResult]   = useState(null)
  const [analyzingAngio, setAnalyzingAngio] = useState(false)
  const [angioError,    setAngioError]    = useState('')

  // initial data load
  useEffect(() => {
    Promise.all([
      getPatient(id).then(({ data }) => setPatient(data)).finally(() => setLoadingPatient(false)),
      getEcgRecords(id).then(({ data }) => setEcgList(data)).finally(() => setLoadingEcg(false)),
      getReports(id).then(({ data }) => setReports(data)).finally(() => setLoadingReports(false)),
    ])
  }, [id])

  const handleUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    setUploadError('')
    try {
      const { data } = await uploadEcg(id, file)
      setEcgList((prev) => [data, ...prev])
      setSelectedEcg(data)
    } catch (err) {
      setUploadError(err.response?.data?.message ?? 'Upload failed.')
    } finally {
      setUploading(false)
      fileRef.current.value = ''
    }
  }

  const handleAngioUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setAnalyzingAngio(true)
    setAngioError('')
    setAngioResult(null)
    try {
      const { data } = await analyzeAngiogram(file)
      setAngioResult(data)
    } catch (err) {
      setAngioError(err.response?.data?.detail ?? err.response?.data?.message ?? 'Analysis failed.')
    } finally {
      setAnalyzingAngio(false)
      angioRef.current.value = ''
    }
  }

  const handleDownload = async (report) => {
    setDownloadingId(report.id)
    try {
      await downloadReport(report.id, `HeartSync-Report-${report.id}.pdf`)
    } catch {
      // silently ignore — browser console will show detail
    } finally {
      setDownloadingId(null)
    }
  }

  // ── render ─────────────────────────────────────────────────────────────────

  if (loadingPatient) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="flex items-center justify-center py-32">
          <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
        </div>
      </div>
    )
  }

  if (!patient) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="max-w-7xl mx-auto px-4 py-16 text-center text-slate-500">
          Patient not found.
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

        {/* Back + heading */}
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/dashboard')}
            className="btn-secondary text-xs px-2.5 py-1.5"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
            Back
          </button>
          <h1 className="text-xl font-bold text-slate-800">
            {patient.firstName} {patient.lastName}
          </h1>
        </div>

        {/* Patient details */}
        <Section title="Patient Information">
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-5">
            <Field label="Date of birth" value={patient.dateOfBirth} />
            <Field label="Gender" value={patient.gender} />
            <Field label="Contact" value={patient.contactNumber} />
            <Field label="Email" value={patient.email} />
            <Field label="Patient ID" value={patient.id} />
          </div>
        </Section>

        <div className="grid lg:grid-cols-2 gap-6">

          {/* ── ECG column ──────────────────────────────────────────────── */}
          <div className="space-y-6">
            <Section title="ECG Studies">
              {/* Upload */}
              <div className="mb-4">
                <input
                  ref={fileRef}
                  type="file"
                  accept=".csv,.txt,.xml,.dcm"
                  className="hidden"
                  onChange={handleUpload}
                />
                <button
                  onClick={() => fileRef.current.click()}
                  disabled={uploading}
                  className="btn-primary w-full justify-center"
                >
                  {uploading ? (
                    <>
                      <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      Uploading…
                    </>
                  ) : (
                    <>
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                      </svg>
                      Upload ECG file
                    </>
                  )}
                </button>
                {uploadError && (
                  <p className="mt-2 text-xs text-red-600">{uploadError}</p>
                )}
              </div>

              {/* ECG list */}
              {loadingEcg ? (
                <div className="flex justify-center py-6">
                  <div className="w-6 h-6 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : ecgList.length === 0 ? (
                <p className="text-sm text-slate-400 text-center py-6">No ECG studies uploaded yet.</p>
              ) : (
                <ul className="space-y-2">
                  {ecgList.map((ecg) => (
                    <li
                      key={ecg.id}
                      onClick={() => setSelectedEcg(ecg)}
                      className={`rounded-lg border px-4 py-3 cursor-pointer transition-colors ${
                        selectedEcg?.id === ecg.id
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-slate-700 truncate pr-3">{ecg.fileName}</span>
                        <StatusBadge status={ecg.status} />
                      </div>
                      <p className="text-xs text-slate-400 mt-0.5">{ecg.uploadedAt?.slice(0, 10)}</p>
                    </li>
                  ))}
                </ul>
              )}
            </Section>

            {/* ECG analysis detail */}
            {selectedEcg && (
              <Section title="ECG Analysis Results">
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-slate-500">Status</span>
                    <StatusBadge status={selectedEcg.status} />
                  </div>

                  {selectedEcg.analysisResult ? (
                    <>
                      <div className="grid grid-cols-2 gap-4">
                        <Field label="Heart rate" value={selectedEcg.analysisResult.heartRate ? `${selectedEcg.analysisResult.heartRate} bpm` : null} />
                        <Field label="Rhythm" value={selectedEcg.analysisResult.rhythm} />
                        <Field label="PR interval" value={selectedEcg.analysisResult.prInterval ? `${selectedEcg.analysisResult.prInterval} ms` : null} />
                        <Field label="QRS duration" value={selectedEcg.analysisResult.qrsDuration ? `${selectedEcg.analysisResult.qrsDuration} ms` : null} />
                        <Field label="QT interval" value={selectedEcg.analysisResult.qtInterval ? `${selectedEcg.analysisResult.qtInterval} ms` : null} />
                        <Field label="QTc" value={selectedEcg.analysisResult.qtcInterval ? `${selectedEcg.analysisResult.qtcInterval} ms` : null} />
                      </div>
                      {selectedEcg.analysisResult.clinicalFindings?.length > 0 && (
                        <div>
                          <p className="text-xs text-slate-400 mb-1">Clinical findings</p>
                          <ul className="space-y-1">
                            {selectedEcg.analysisResult.clinicalFindings.map((f, i) => (
                              <li key={i} className="text-sm text-slate-700 flex items-start gap-2">
                                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-blue-500 flex-shrink-0" />
                                {f}
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </>
                  ) : (
                    <p className="text-sm text-slate-400 py-2">Analysis pending or not yet available.</p>
                  )}
                </div>
              </Section>
            )}
          </div>

          {/* ── AI + Reports column ─────────────────────────────────────── */}
          <div className="space-y-6">

            {/* AI Angiogram Analysis */}
            <Section title="AI Angiogram Analysis">
              <div className="mb-4">
                <input
                  ref={angioRef}
                  type="file"
                  accept=".dcm,.png,.jpg,.jpeg,.tiff,.tif"
                  className="hidden"
                  onChange={handleAngioUpload}
                />
                <button
                  onClick={() => angioRef.current.click()}
                  disabled={analyzingAngio}
                  className="btn-primary w-full justify-center"
                >
                  {analyzingAngio ? (
                    <>
                      <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      Analyzing…
                    </>
                  ) : (
                    <>
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                      </svg>
                      Upload Angiogram Image
                    </>
                  )}
                </button>
                {angioError && (
                  <p className="mt-2 text-xs text-red-600">{angioError}</p>
                )}
              </div>

              {!angioResult && !analyzingAngio && (
                <p className="text-sm text-slate-400 text-center py-4">
                  Upload a coronary angiogram (.dcm, .png, .jpg) to run segmentation and stenosis analysis.
                </p>
              )}

              {angioResult && (
                <div className="space-y-5">

                  {/* Stats row */}
                  <div className="grid grid-cols-3 gap-4">
                    <div>
                      <p className="text-xs text-slate-400 mb-1">Overall Risk</p>
                      <RiskBadge risk={angioResult.overallRisk} />
                    </div>
                    <div>
                      <p className="text-xs text-slate-400 mb-1">Branches Detected</p>
                      <p className="text-sm font-semibold text-slate-800">{angioResult.totalBranches}</p>
                    </div>
                    <div>
                      <p className="text-xs text-slate-400 mb-1">Model Confidence</p>
                      <p className="text-sm font-semibold text-slate-800">
                        {(angioResult.confidence * 100).toFixed(1)}%
                      </p>
                    </div>
                  </div>

                  {/* Segmentation overlay */}
                  {angioResult.overlayBase64 && (
                    <div>
                      <p className="text-xs text-slate-400 mb-2">Segmentation Overlay</p>
                      <img
                        src={`data:image/png;base64,${angioResult.overlayBase64}`}
                        alt="Coronary segmentation overlay"
                        className="w-full rounded-lg border border-slate-200"
                      />
                    </div>
                  )}

                  {/* Lesion table */}
                  {angioResult.lesions?.length > 0 ? (
                    <div>
                      <p className="text-xs text-slate-400 mb-2">
                        Detected Lesions ({angioResult.lesions.length})
                      </p>
                      <div className="overflow-x-auto rounded-lg border border-slate-100">
                        <table className="w-full text-xs">
                          <thead className="bg-slate-50">
                            <tr className="text-slate-500">
                              <th className="text-left px-3 py-2 font-medium">#</th>
                              <th className="text-left px-3 py-2 font-medium">Severity</th>
                              <th className="text-right px-3 py-2 font-medium">DS%</th>
                              <th className="text-right px-3 py-2 font-medium">MLD (px)</th>
                              <th className="text-right px-3 py-2 font-medium">RVD (px)</th>
                              <th className="text-right px-3 py-2 font-medium">Length (px)</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-50">
                            {angioResult.lesions.map((l) => (
                              <tr key={l.rank} className="hover:bg-slate-50">
                                <td className="px-3 py-2 text-slate-500">{l.rank}</td>
                                <td className="px-3 py-2"><SeverityBadge severity={l.severity} /></td>
                                <td className="px-3 py-2 text-right font-semibold text-slate-700">
                                  {l.dsPercent.toFixed(1)}%
                                </td>
                                <td className="px-3 py-2 text-right text-slate-600">{l.mldPx.toFixed(1)}</td>
                                <td className="px-3 py-2 text-right text-slate-600">{l.rvdPx.toFixed(1)}</td>
                                <td className="px-3 py-2 text-right text-slate-600">{l.lengthPx.toFixed(1)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  ) : (
                    <p className="text-xs text-slate-400 text-center py-2">No significant lesions detected.</p>
                  )}

                  <p className="text-xs text-slate-300">
                    Segmentation: {angioResult.segmentationTimeMs}ms · QCA: {angioResult.qcaTimeMs}ms
                  </p>
                </div>
              )}
            </Section>

            {/* Reports */}
            <Section title="Clinical Reports">
              {loadingReports ? (
                <div className="flex justify-center py-6">
                  <div className="w-6 h-6 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : reports.length === 0 ? (
                <p className="text-sm text-slate-400 text-center py-6">
                  Reports are auto-generated after ECG upload and AI analysis complete.
                </p>
              ) : (
                <ul className="space-y-2">
                  {reports.map((r) => (
                    <li key={r.id} className="rounded-lg border border-slate-200 px-4 py-3">
                      <div className="flex items-center justify-between gap-3">
                        <div className="flex items-center gap-3 min-w-0">
                          <svg className="w-8 h-8 text-red-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                            <path strokeLinecap="round" strokeLinejoin="round" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                          </svg>
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-slate-700 truncate">Report #{r.id.slice(-8)}</p>
                            <p className="text-xs text-slate-400">{r.generatedAt?.slice(0, 10)} · <StatusBadge status={r.status} /></p>
                          </div>
                        </div>
                        {r.status === 'COMPLETED' && (
                          <button
                            onClick={() => handleDownload(r)}
                            disabled={downloadingId === r.id}
                            className="btn-secondary text-xs px-3 py-1.5 flex-shrink-0"
                          >
                            {downloadingId === r.id ? (
                              <span className="w-3.5 h-3.5 border-2 border-slate-400 border-t-transparent rounded-full animate-spin" />
                            ) : (
                              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                                <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                              </svg>
                            )}
                            Download PDF
                          </button>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </Section>

          </div>
        </div>
      </main>
    </div>
  )
}
