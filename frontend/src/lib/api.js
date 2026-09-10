import { useEffect, useState } from 'react'
export function queryString(values = {}) {
  return new URLSearchParams(Object.entries(values).filter(([, value]) => value !== '' && value != null)).toString()
}
export async function api(path, { token, body, ...options } = {}) {
  const headers = { ...options.headers }
  if (token) headers.Authorization = `Bearer ${token}`
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json'
  const response = await fetch(`/api${path}`, { ...options, headers, body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined })
  if (response.status === 204) return null
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401 && token) window.dispatchEvent(new Event('auth-expired'))
    throw new Error([data?.message || `요청을 처리하지 못했습니다 (${response.status})`, ...Object.values(data?.fieldErrors || data?.details || {})].join(' · '))
  }
  return data
}
export function useResource(path, revision = 0, token = '') {
  const key = JSON.stringify([path, revision, token])
  const [state, setState] = useState({})
  useEffect(() => {
    if (!path) return
    const controller = new AbortController()
    api(path, { signal: controller.signal, token }).then(data => setState({ key, data }))
      .catch(error => { if (!controller.signal.aborted) setState({ key, error: error.message }) })
    return () => controller.abort()
  }, [path, revision, token, key])
  return state.key === key ? { ...state, loading: false } : { data: null, error: null, loading: !!path }
}
export const number = value => value == null ? '—' : Number(value).toLocaleString('ko-KR')
export const date = value => value ? new Date(value).toLocaleDateString('ko-KR') : '—'
export const geoLabels = { PENDING: '좌표 확인 대기', RESOLVED: '위치 확인 완료', NOT_FOUND: '주소 검색 결과 없음', REVIEW: '주소 확인 필요', FAILED: '좌표 조회 실패' }
