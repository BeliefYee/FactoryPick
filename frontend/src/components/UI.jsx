import { useEffect, useRef } from 'react'
export function Icon({ name = 'factory', size = 20 }) {
  const paths = {
    factory: 'M3 21V9l6 3V7l6 3V3h5v18H3ZM7 16v2m5-2v2m5-2v2',
    map: 'm3 5 6-2 6 2 6-2v16l-6 2-6-2-6 2V5Zm6-2v16m6-14v16',
    search: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z',
    box: 'm3 7 9-5 9 5v10l-9 5-9-5V7Zm0 0 9 5 9-5m-9 5v10M7 4l10 5',
    chart: 'M3 3v18h18M7 16v-4m5 4V7m5 9V4', data: 'M4 5h16v14H4V5Zm0 5h16M9 5v14',
    close: 'm6 6 12 12M6 18 18 6', arrow: 'M5 12h14m-5-5 5 5-5 5',
    pin: 'M19 10c0 5-7 12-7 12S5 15 5 10a7 7 0 0 1 14 0ZM15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
    refresh: 'M20 7A9 9 0 1 0 21 15M20 2v5h-5', user: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2', plus: 'M12 5v14M5 12h14',
  }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name] || paths.factory} /></svg>
}
export function State({ loading, error, empty, children }) {
  if (loading) return <div className="empty" role="status"><span className="spinner" /> 데이터를 불러오는 중입니다.</div>
  if (error) return <div className="empty error" role="alert">{error}</div>
  if (empty) return <div className="empty">조회된 데이터가 없습니다.</div>
  return children
}
export function Modal({ title, onClose, children, wide = false }) {
  const ref = useRef(null)
  useEffect(() => { const dialog = ref.current; dialog.showModal(); return () => dialog.close() }, [])
  return <dialog ref={ref} className={`modal ${wide ? 'wide' : ''}`} onCancel={onClose} aria-label={title}><div className="panel-heading"><h2>{title}</h2><button className="icon-button" onClick={onClose} aria-label="닫기"><Icon name="close" /></button></div>{children}</dialog>
}
export function Pagination({ page, totalPages = 0, onChange }) {
  return <div className="pagination"><button disabled={page <= 0} onClick={() => onChange(page - 1)}>이전</button><span>{totalPages ? page + 1 : 0} / {totalPages}</span><button disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>다음</button></div>
}
export function Field({ label, children }) { return <label className="field"><span>{label}</span>{children}</label> }
