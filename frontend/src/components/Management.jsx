import { useState } from 'react'
import { api, useResource, queryString, number, date } from '../lib/api'
import { Field, Icon, Modal, Pagination, State } from './UI'

export function Login({ onClose, onLogin }) {
  const [error, setError] = useState(''), [busy, setBusy] = useState(false)
  const submit = async e => {
    e.preventDefault(); setBusy(true); setError('')
    const form = new FormData(e.currentTarget)
    try { const result = await api('/admin/auth/login', { method: 'POST', body: Object.fromEntries(form) }); onLogin({ ...result, username: form.get('username') }); onClose() }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <Modal title="관리자 로그인" onClose={onClose}><p className="muted">공장·제품 등록과 데이터 관리는 관리자 계정으로 이용합니다.</p><form className="form-stack" onSubmit={submit}><Field label="아이디"><input name="username" autoComplete="username" required autoFocus /></Field><Field label="비밀번호"><input name="password" type="password" autoComplete="current-password" required /></Field>{error && <p className="notice error" role="alert">{error}</p>}<button className="primary block" disabled={busy}>{busy ? '로그인 중…' : '로그인'}</button></form></Modal>
}

const factoryFields = [
  ['factoryName', '공장명', 'text', 255], ['companyName', '기업명', 'text', 150], ['address', '주소', 'text', 1000],
  ['sido', '시·도', 'text', 50], ['sigungu', '시·군·구', 'text', 80], ['phone', '전화번호', 'text', 100],
  ['businessNumber', '사업자번호', 'text', 30], ['industry', '업종', 'text', 10000], ['establishedYear', '설립 연도', 'number'],
  ['factoryScale', '공장 규모', 'text', 50], ['latitude', '위도', 'number'], ['longitude', '경도', 'number'],
]
export function Editor({ kind, initial, token, onClose, onSaved }) {
  const isFactory = kind === 'factories', value = isFactory ? initial?.factory || {} : initial || {}
  const [error, setError] = useState(''), [busy, setBusy] = useState(false), [search, setSearch] = useState('')
  const [selected, setSelected] = useState(initial?.products || [])
  const choices = useResource(isFactory ? `/products?${queryString({ keyword: search, size: 50 })}` : null)
  const id = isFactory ? value.factoryId : value.productId
  const submit = async e => {
    e.preventDefault(); setError('')
    const values = Object.fromEntries(new FormData(e.currentTarget))
    Object.keys(values).forEach(k => { values[k] = values[k].trim() || null })
    if (isFactory) {
      for (const field of ['latitude', 'longitude', 'establishedYear']) if (values[field] != null) values[field] = Number(values[field])
      if ((values.latitude == null) !== (values.longitude == null)) { setError('위도와 경도를 함께 입력하거나 둘 다 비워 주세요.'); return }
    }
    setBusy(true)
    try { await api(`/admin/${kind}${id ? `/${id}` : ''}`, { token, method: id ? 'PUT' : 'POST', body: isFactory ? { factory: values, productIds: selected.map(p => p.productId) } : values }); onSaved(); onClose() }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <Modal title={`${isFactory ? '공장' : '제품'} ${id ? '수정' : '등록'}`} onClose={() => !busy && onClose()} wide={isFactory}><form onSubmit={submit} className="form-stack"><div className="form-grid">{(isFactory ? factoryFields : [['productName','제품명','text',150],['category','카테고리','text',100]]).map(([name,label,type,max]) => <Field key={name} label={`${label}${['factoryName','productName'].includes(name) ? ' *' : ''}`}><input name={name} defaultValue={value[name] ?? ''} type={type} step={['latitude','longitude'].includes(name) ? 'any' : undefined} min={name === 'latitude' ? 33 : name === 'longitude' ? 124 : name === 'establishedYear' ? 1800 : undefined} max={name === 'latitude' ? 39 : name === 'longitude' ? 132 : name === 'establishedYear' ? 2100 : undefined} maxLength={max} required={['factoryName','productName'].includes(name)} /></Field>)}</div>
    {!isFactory && <Field label="제품 설명"><textarea name="description" defaultValue={value.description || ''} maxLength={500} rows={4} /></Field>}
    {isFactory && <><p className="fine-print">좌표를 모르면 비워두고 저장한 뒤 ‘주소로 좌표 찾기’를 사용할 수 있습니다.</p><h3>생산 제품 연결 <span className="count-badge">{selected.length}</span></h3><div className="tags">{selected.map(p => <button type="button" className="tag" key={p.productId} onClick={() => setSelected(selected.filter(x => x.productId !== p.productId))}>{p.productName} ×</button>)}</div><input aria-label="연결할 제품 검색" value={search} onChange={e => setSearch(e.target.value)} placeholder="제품명을 검색해 연결하세요" /><State {...choices} empty={!choices.data?.content?.length}><div className="choice-list">{choices.data?.content.map(p => <label key={p.productId}><input type="checkbox" checked={selected.some(x => x.productId === p.productId)} onChange={e => setSelected(e.target.checked ? [...selected,p] : selected.filter(x => x.productId !== p.productId))} /><span>{p.productName}<small>{p.category || '미분류'}</small></span></label>)}</div>{choices.data?.totalElements > 50 && <p className="fine-print">검색 결과 중 50개를 표시합니다. 제품명을 입력해 범위를 좁혀 주세요.</p>}</State></>}
    {error && <p className="notice error" role="alert">{error}</p>}<div className="action-row"><button type="button" onClick={onClose} disabled={busy}>취소</button><button className="primary" disabled={busy}>{busy ? '저장 중…' : '저장'}</button></div></form></Modal>
}

export function DeleteDialog({ target, token, onClose, onDeleted }) {
  const [busy,setBusy] = useState(false), [error,setError] = useState('')
  const remove = async () => { setBusy(true); try { await api(`/admin/${target.kind}/${target.id}`, { method: 'DELETE',token }); onDeleted(); onClose() } catch(e) { setError(e.message) } finally { setBusy(false) } }
  return <Modal title="데이터 삭제" onClose={() => !busy && onClose()}><p><strong>{target.name}</strong>을 삭제하시겠습니까?</p><p className="muted">연결된 생산 관계도 해제됩니다. 이 작업은 되돌릴 수 없습니다.</p>{error && <p className="notice error">{error}</p>}<div className="action-row"><button disabled={busy} onClick={onClose}>취소</button><button className="danger" disabled={busy} onClick={remove}>{busy ? '삭제 중…' : '삭제'}</button></div></Modal>
}

export default function Management({ token, revision, onRefresh, onFactory, onProduct, onCreate, onEditFactory, onEditProduct, onDelete }) {
  const [tab,setTab] = useState('factories'), [search,setSearch] = useState(''), [page,setPage] = useState(0)
  const [busy,setBusy] = useState(false), [result,setResult] = useState(null), [error,setError] = useState('')
  const resource = useResource(token ? tab === 'imports' ? '/admin/data/imports' : `/admin/${tab}?${queryString({ keyword:search,page,size:15 })}` : null, revision, token)
  if (!token) return null
  const upload = async e => {
    e.preventDefault(); setBusy(true); setError(''); setResult(null)
    const body = new FormData(e.currentTarget)
    try { setResult(await api('/admin/data/imports/csv', {method:'POST',body,token})); onRefresh() } catch(e) { setError(e.message) } finally { setBusy(false) }
  }
  const editFactory = async id => { try { onEditFactory(await api(`/factories/${id}`)) } catch(e) { setError(e.message) } }
  return <section className="panel management"><div className="view-heading"><div><span className="eyebrow">DATA MANAGEMENT</span><h2>데이터 관리</h2></div><div className="segmented">{[['factories','공장'],['products','제품'],['imports','CSV 업로드']].map(([key,label]) => <button key={key} className={tab===key?'active':''} onClick={() => {setTab(key);setPage(0);setSearch('');setError('')}}>{label}</button>)}</div></div>
    {error && <p className="notice error" role="alert">{error}</p>}
    {tab === 'imports' ? <><form className="upload-box" onSubmit={upload}><Icon name="data" size={32} /><div><h3>CSV 공장 데이터 가져오기</h3><p className="muted">UTF-8 CSV · 최대 20MB · 공장명 필수</p></div><input type="file" name="file" accept=".csv,text/csv" required aria-label="CSV 파일" /><button className="primary" disabled={busy}>{busy?'업로드 중…':'데이터 등록'}</button></form><p className="fine-print">열 이름: factory_name, company_name, address, sido, sigungu, latitude, longitude, business_number, industry, established_year, factory_scale, phone, product_name, category, product_description</p>
      {result && <div className="notice" role="status">전체 {result.totalRows}행 · 신규 {result.insertedRows} · 갱신 {result.updatedRows} · 건너뜀 {result.skippedRows} · 실패 {result.failedRows}<p>{result.message}</p></div>}
      <h3 className="section-title">업로드 이력</h3><State {...resource} empty={!resource.data?.length}><div className="table-wrap"><table><thead><tr><th>파일명</th><th>등록일</th><th>전체</th><th>신규 / 갱신</th><th>실패</th><th>상태</th></tr></thead><tbody>{resource.data?.map(row => <tr key={row.importId}><td><strong>{row.sourceName}</strong><small className="cell-description">{row.message}</small></td><td>{date(row.importedAt)}</td><td>{number(row.totalRows)}</td><td>{number(row.insertedRows)} / {number(row.updatedRows)}</td><td>{number(row.failedRows)}</td><td><span className={`tag ${row.failedRows ? 'warning' : ''}`}>{({SUCCESS:'완료',PARTIAL:'부분 완료',FAILED:'실패'})[row.status] || row.status}</span></td></tr>)}</tbody></table></div></State></>
      : <><div className="table-toolbar"><input aria-label="관리 데이터 검색" placeholder={`${tab==='factories'?'공장·기업':'제품'}명 검색`} value={search} onChange={e => {setSearch(e.target.value);setPage(0)}} /><button className="primary" onClick={() => onCreate(tab)}><Icon name="plus" />{tab==='factories'?'공장':'제품'} 등록</button></div><State {...resource} empty={!resource.data?.content?.length}><div className="table-wrap"><table><thead><tr><th>이름</th><th>{tab==='factories'?'지역 / 주소':'카테고리'}</th><th>관리</th></tr></thead><tbody>{resource.data?.content.map(row => <tr key={row.factoryId || row.productId}><td><button className="text-button" onClick={() => tab==='factories'?onFactory(row.factoryId):onProduct(row.productId)}>{row.factoryName || row.productName}</button></td><td>{row.address || row.category || '미등록'}</td><td><div className="action-row compact"><button onClick={() => tab==='factories'?editFactory(row.factoryId):onEditProduct(row)}>수정</button><button className="danger-text" onClick={() => onDelete({kind:tab,id:row.factoryId || row.productId,name:row.factoryName || row.productName})}>삭제</button></div></td></tr>)}</tbody></table></div></State><Pagination page={page} totalPages={resource.data?.totalPages} onChange={setPage} /></>}
  </section>
}
