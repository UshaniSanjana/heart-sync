import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

const AUTH_ENDPOINTS = ['/auth/login', '/auth/register']

api.interceptors.request.use((config) => {
  const isAuthEndpoint = AUTH_ENDPOINTS.some(ep => config.url?.startsWith(ep))
  if (!isAuthEndpoint) {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

// --- Auth ---
export const login = (email, password) =>
  api.post('/auth/login', { email, password })

export const register = (fullName, email, password, role) =>
  api.post('/auth/register', { fullName, email, password, role })

// --- Patients ---
export const getPatients = () => api.get('/patients')

export const getPatient = (id) => api.get(`/patients/${id}`)

export const createPatient = (data) => api.post('/patients', data)

// --- ECG ---
export const uploadEcg = (patientId, file) => {
  const form = new FormData()
  form.append('patientId', patientId)
  form.append('file', file)
  return api.post('/ecg/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const getEcgRecords = (patientId) =>
  api.get(`/ecg/patient/${patientId}`)

export const getEcgRecord = (id) => api.get(`/ecg/${id}`)

// --- AI Results ---
export const getAiResult = (ecgId) => api.get(`/ai/results/ecg/${ecgId}`)

export const analyzeAngiogram = (file) => {
  const form = new FormData()
  form.append('file', file)
  return api.post('/ai/angiogram/analyze', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// --- Reports ---
export const getReports = (patientId) =>
  api.get(`/reports/patient/${patientId}`)

export const downloadReport = async (reportId, fileName) => {
  const response = await api.get(`/reports/${reportId}/download`, {
    responseType: 'blob',
  })
  const url = window.URL.createObjectURL(
    new Blob([response.data], { type: 'application/pdf' })
  )
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', fileName || `report-${reportId}.pdf`)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

export default api
