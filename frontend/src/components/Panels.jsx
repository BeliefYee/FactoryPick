import { useState } from 'react'
import { api, useResource, number, date, geoLabels } from '../lib/api'
import { Icon, State, Modal } from './UI'

const colors = ['#3495ff', '#826de5', '#22bbad', '#f5ae4d', '#df779e', '#70bb6b', '#62b8dc', '#b98ddb']
export function StatsPanel({ title, resource, donut = false, limit = 5, onPick }) {
  const rows = resource.data || [], shown = rows.slice(0, limit), max = Math.max(1, ...rows.map(x => x.count))
  const total = rows.reduce((sum, row) => sum + row.count, 0)
  let offset = 0
  const gradient = rows.map((row, i) => { const start = offset; offset += total ? row.count / total * 100 : 0; return `${colors[i % colors.length]} ${start}% ${offset}%` }).join(',')
  return <section className="panel stats-panel"><div className="panel-heading"><h3>{title}</h3><span className="eyebrow">전체 DB 기준</span></div><State {...resource} empty={!rows.length}>
    <div className={donut ? 'donut-layout' : ''}>{donut && <div className="donut" style={{ background: total ? `conic-gradient(${gradient})` : '#25394a' }}><div><span>제품 분류</span><strong>{rows.length}</strong><small>카테고리</small></div></div>}
      <div className="rank-list">{shown.map((row, i) => <button className="rank-row" key={row.label} onClick={() => onPick?.(row.label)} disabled={!onPick || row.label === '미분류'}>
        <span className="rank-number" style={donut ? { background: colors[i % colors.length] } : {}}>{donut ? '' : i + 1}</span><span className="rank-name">{row.label}</span>{!donut && <span className="bar"><i style={{ width: `${row.count / max * 100}%` }} /></span>}<strong>{number(row.count)}<small> 곳</small></strong>
      </button>)}</div></div>
    {donut && <p className="fine-print">복수 카테고리를 생산하는 공장은 각 분류에 포함됩니다.</p>}
  </State></section>
}

function Values({ items }) {
  return <dl className="detail-values">{items.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value == null || value === '' ? '미등록' : value}</dd></div>)}</dl>
}
export function FactoryDetail({ resource, onClose, onProduct, token, onEdit, onDelete, onRefresh, full = false }) {
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
      <div className="tags">{[...new Set(data.products.map(p => p.category || '미분류'))].map(c => <span className="tag" key={c}>{c}</span>)}</div>
      <Values items={[["위치", f.address], ["기업명", f.companyName], ["업종", f.industry], ["대표자", f.representativeName], ["임직원 수", f.employeeCount == null ? null : `${number(f.employeeCount)}명`], ["설립 연도", f.establishedYear ? `${f.establishedYear}년` : null], ["최초 등록일", f.firstRegisteredDate ? date(f.firstRegisteredDate) : null], ["전화번호", f.phone], ["산업단지", f.industrialComplexName]]} />
      <div className="location-state"><Icon name="pin" /><span>{geoLabels[f.geocodingStatus] || '위치 미확인'}</span></div>
      {token && f.latitude == null && <button className="button block" onClick={geocode} disabled={busy}>{busy ? '주소를 조회하는 중…' : '주소로 좌표 찾기'}</button>}
      {feedback && <p className="notice" role="status">{feedback}</p>}
      <div className="detail-section"><h3>생산 제품 <span className="count-badge">{data.products.length}</span></h3><div className="product-cards">{data.products.map(p => <button key={p.productId} onClick={() => onProduct(p.productId)}><Icon name="box" size={25} /><strong>{p.productName}</strong><small>{p.category || '미분류'}</small></button>)}</div>{!data.products.length && <p className="muted">연결된 제품이 없습니다.</p>}{f.mainProductText && <p className="raw-products">{f.mainProductText}</p>}</div>
      <details className="more-detail" open={full}><summary>상세 등록 정보</summary><Values items={[["공장관리번호", f.factoryManageNo], ["사업자번호", f.businessNumber], ["공장 규모", f.factoryScale], ["대표 업종코드", f.primaryIndustryCode], ["담당 기관", f.managingAgencyName], ["팩스", f.faxNumber], ["위도 / 경도", f.latitude != null ? `${f.latitude} / ${f.longitude}` : null], ["데이터 갱신", date(f.updatedAt)], ["최종 수집", f.lastSyncedAt ? date(f.lastSyncedAt) : null]]} />{homepage && <a href={homepage} target="_blank" rel="noreferrer">홈페이지 방문 ↗</a>}</details>
      {token && <div className="action-row"><button onClick={() => onEdit(data)}>공장 수정</button><button className="danger-text" onClick={() => onDelete({ kind: 'factories', id: f.factoryId, name: f.factoryName })}>삭제</button></div>}
    </> : <div className="empty selection-empty"><Icon name="pin" size={38} /><h3>공장을 선택해 주세요</h3><p>지도 마커나 공장 목록에서 선택하면<br />위치와 생산 제품을 확인할 수 있습니다.</p></div>}</State>
  </section>
}

export function ProductDetail({ id, onClose, onFactory, token, onEdit, onDelete, revision }) {
  const product = useResource(`/products/${id}`, revision), factories = useResource(`/products/${id}/factories`, revision)
  const p = product.data
  return <Modal title="제품 상세 정보" onClose={onClose}><State {...product}>{p && <><span className="tag">{p.category || '미분류'}</span><h2 className="modal-title">{p.productName}</h2><p className="muted">{p.description || '등록된 제품 설명이 없습니다.'}</p>{token && <div className="action-row"><button onClick={() => onEdit(p)}>제품 수정</button><button className="danger-text" onClick={() => onDelete({ kind: 'products', id, name: p.productName })}>삭제</button></div>}</>}</State><h3 className="section-title">이 제품을 생산하는 공장</h3><State {...factories} empty={!factories.data?.length}><div className="compact-list">{factories.data?.map(f => <button key={f.factoryId} onClick={() => { onFactory(f.factoryId); onClose() }}><Icon /><span><strong>{f.factoryName}</strong><small>{f.address || '주소 미등록'}</small></span><Icon name="arrow" /></button>)}</div></State></Modal>
}
