import { useState } from 'react'
import { api, number, date, geoLabels } from '../lib/api'
import { Icon, State } from './UI'
import CategoryIcon from './CategoryIcon'
import { getCategoryIcon } from '../lib/categoryIcons'

export function StatsPanel({ title, resource, donut = false, limit = 5, onPick }) {
  const rows = resource.data || [], shown = rows.slice(0, limit), max = Math.max(1, ...rows.map(x => x.count))
  const total = rows.reduce((sum, row) => sum + row.count, 0)
  let offset = 0
  const gradient = rows.map(row => { const start = offset; offset += total ? row.count / total * 100 : 0; return `${getCategoryIcon(row.label).color} ${start}% ${offset}%` }).join(',')
  return <section className="panel stats-panel"><div className="panel-heading"><h3>{title}</h3><span className="eyebrow">전체 DB 기준</span></div><State {...resource} empty={!rows.length}>
    <div className={donut ? 'donut-layout' : ''}>{donut && <div className="donut" style={{ background: total ? `conic-gradient(${gradient})` : '#25394a' }}><div><span>공장 분류</span><strong>{rows.length}</strong><small>카테고리</small></div></div>}
      <div className="rank-list">{shown.map((row, i) => <button className="rank-row" key={row.label} onClick={() => onPick?.(row.label)} disabled={!onPick || row.label === '미분류'}>
        {donut ? <CategoryIcon category={row.label} size={24}/> : <span className="rank-number">{i + 1}</span>}<span className="rank-name">{row.label}</span>{!donut && <span className="bar"><i style={{ width: `${row.count / max * 100}%` }} /></span>}<strong>{number(row.count)}<small> 곳</small></strong>
      </button>)}</div></div>
    {donut && <p className="fine-print">대표 업종코드를 기준으로 공장을 분류합니다.</p>}
  </State></section>
}

function Values({ items }) {
  return <dl className="detail-values">{items.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value == null || value === '' ? '미등록' : value}</dd></div>)}</dl>
}
export function FactoryDetail({ resource, onClose, token, onEdit, onDelete, onRefresh, full = false }) {
  const [busy, setBusy] = useState(false), [feedback, setFeedback] = useState('')
  const data = resource.data, f = data?.factory
  const geocode = async () => {
    setBusy(true); setFeedback('')
    try { const result = await api(`/admin/factories/${f.factoryId}/geocode`, { method: 'POST', token }); setFeedback(geoLabels[result.geocodingStatus]); onRefresh() }
    catch (e) { setFeedback(e.message) } finally { setBusy(false) }
  }
  let homepage
  try { const url = new URL(f?.homepageRaw); if (['https:', 'http:'].includes(url.protocol)) homepage = url.href } catch { /* raw field is not always a URL */ }
  return <section className={`panel detail-panel ${full ? 'full-detail' : ''}`}><div className="panel-heading"><h3>선택된 공장 정보</h3>{onClose && <button className="icon-button" onClick={onClose} aria-label="공장 선택 해제"><Icon name="close" /></button>}</div>
    <State {...resource}>{f ? <><div className="factory-heading"><span className="entity-icon"><Icon size={27} /></span><div><span className="eyebrow">FACTORY INFORMATION</span><h2>{f.factoryName}</h2></div></div>
      <div className="tags">{(data.categories?.length ? data.categories : ['미분류']).map(c => <span className="tag" key={c}><CategoryIcon category={c} size={22}/>{c}</span>)}</div>
      <Values items={[["위치", f.address], ["기업명", f.companyName], ["업종", f.industry], ["대표자", f.representativeName], ["임직원 수", f.employeeCount == null ? null : `${number(f.employeeCount)}명`], ["설립 연도", f.establishedYear ? `${f.establishedYear}년` : null], ["최초 등록일", f.firstRegisteredDate ? date(f.firstRegisteredDate) : null], ["전화번호", f.phone], ["산업단지", f.industrialComplexName]]} />
      <div className="location-state"><Icon name="pin" /><span>{geoLabels[f.geocodingStatus] || '위치 미확인'}</span></div>
      {token && f.latitude == null && <button className="button block" onClick={geocode} disabled={busy}>{busy ? '주소를 조회하는 중…' : '주소로 좌표 찾기'}</button>}
      {feedback && <p className="notice" role="status">{feedback}</p>}
      <details className="more-detail" open={full}><summary>상세 등록 정보</summary><Values items={[["공장관리번호", f.factoryManageNo], ["사업자번호", f.businessNumber], ["공장 규모", f.factoryScale], ["대표 업종코드", f.primaryIndustryCode], ["담당 기관", f.managingAgencyName], ["팩스", f.faxNumber], ["위도 / 경도", f.latitude != null ? `${f.latitude} / ${f.longitude}` : null], ["데이터 갱신", date(f.updatedAt)], ["최종 수집", f.lastSyncedAt ? date(f.lastSyncedAt) : null]]} />{homepage && <a href={homepage} target="_blank" rel="noreferrer">홈페이지 방문 ↗</a>}</details>
      {token && <div className="action-row"><button onClick={() => onEdit(data)}>공장 수정</button><button className="danger-text" onClick={() => onDelete({ kind: 'factories', id: f.factoryId, name: f.factoryName })}>삭제</button></div>}
    </> : <div className="empty selection-empty"><Icon name="pin" size={38} /><h3>공장을 선택해 주세요</h3><p>지도 마커나 공장 목록에서 선택하면<br />위치와 상세 정보를 확인할 수 있습니다.</p></div>}</State>
  </section>
}
