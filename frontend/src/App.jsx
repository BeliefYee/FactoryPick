import { useCallback, useEffect, useState } from 'react'
import FactoryMap from './components/FactoryMap'
import CategoryIcon from './components/CategoryIcon'
import { FactoryDetail, ProductDetail, StatsPanel } from './components/Panels'
import Management, { DeleteDialog, Editor, Login } from './components/Management'
import { Field, Icon, Pagination, State } from './components/UI'
import { api, number, queryString, useResource } from './lib/api'
import './App.css'

const emptyFilters = { keyword:'', sido:'', sigungu:'', product:'', category:'' }
const tabs = [['map','지도','map'],['factories','공장 목록','factory'],['products','제품 목록','box'],['statistics','통계 분석','chart'],['management','데이터 관리','data']]
function readSession() { try { const s=JSON.parse(sessionStorage.getItem('factorypick-session')); return s && new Date(s.expiresAt)>new Date()?s:null } catch {return null} }

export default function App() {
  const [view,setView]=useState('map'), [filters,setFilters]=useState(emptyFilters), [draft,setDraft]=useState(emptyFilters)
  const [revision,setRevision]=useState(0), [page,setPage]=useState(0), [productPage,setProductPage]=useState(0)
  const [selectedId,setSelectedId]=useState(null), [productId,setProductId]=useState(null), [mapCount,setMapCount]=useState(null)
  const [session,setSession]=useState(readSession), [login,setLogin]=useState(false), [editor,setEditor]=useState(null), [deleting,setDeleting]=useState(null)
  const [notice,setNotice]=useState(''), [productKeyword,setProductKeyword]=useState('')
  const token=session?.token || ''
  const refresh=useCallback(() => setRevision(x=>x+1), [])
  const endSession=useCallback(() => {
    setSession(null)
    sessionStorage.removeItem('factorypick-session')
    setView(current=>current==='management'?'map':current)
    setEditor(null)
    setDeleting(null)
  },[])
  const clearSession=useCallback(() => {endSession();setNotice('관리자 세션이 종료되었습니다. 다시 로그인해 주세요.')},[endSession])
  useEffect(() => {window.addEventListener('auth-expired',clearSession);return()=>window.removeEventListener('auth-expired',clearSession)},[clearSession])
  useEffect(() => {if(!session)return;const timer=setTimeout(clearSession,Math.max(0,new Date(session.expiresAt)-Date.now()));return()=>clearTimeout(timer)},[session,clearSession])
  const categories=useResource('/factories/categories',revision), regions=useResource('/statistics/regions',revision)
  const categoryStats=useResource('/statistics/categories',revision), productStats=useResource('/statistics/products',revision)
  const mapCategories=[...new Set([filters.category,...(categoryStats.data||[]).filter(row=>row.label!=='미분류').slice(0,6).map(row=>row.label)])].filter(Boolean)
  const health=useResource('/health',revision)
  const query=queryString(filters)
  const factories=useResource(`/factories?${query}&page=${page}&size=15`,revision)
  const products=useResource(`/products?${queryString({keyword:productKeyword,category:filters.category,page:productPage,size:15})}`,revision)
  const detail=useResource(selectedId ? `/factories/${selectedId}` : null,revision)
  const totalFactories=useResource('/factories?size=1',revision), totalProducts=useResource('/products?size=1',revision)
  const apply = next => {setFilters(next);setDraft(next);setPage(0);setProductPage(0);setSelectedId(null);setMapCount(null)}
  const category = value => apply({...filters,category:value})
  const selectFactory = id => {setSelectedId(id);setView('map')}
  const editFactory = data => {setProductId(null);setEditor({kind:'factories',initial:data})}
  const editProduct = data => {setProductId(null);setEditor({kind:'products',initial:data})}
  const logout = async () => {try{await api('/admin/auth/logout',{method:'POST',token});endSession();setNotice('')}catch(e){setNotice(e.message)}}
  const loginSuccess = s => {setSession(s);sessionStorage.setItem('factorypick-session',JSON.stringify(s));setNotice('')}
  const reset = () => apply(emptyFilters)
  return <div className="app-shell">
    <header className="app-header"><a className="brand" href="#" onClick={e=>{e.preventDefault();setView('map')}}><span className="brand-symbol"><Icon size={31}/></span><div><h1>전국 공장 · 제품 분포 조회 시스템</h1><p>공장과 생산 제품을 한눈에 확인하세요</p></div></a>
      <nav aria-label="주 메뉴">{tabs.filter(([id])=>id!=='management'||token).map(([id,label,icon])=><button key={id} className={view===id?'active':''} aria-current={view===id?'page':undefined} onClick={()=>{setView(id);setPage(0)}}><Icon name={icon}/><span>{label}</span></button>)}</nav>
      <div className="header-actions"><button className="icon-button" title="데이터 새로고침" aria-label="데이터 새로고침" onClick={refresh}><Icon name="refresh"/></button><button className="account" onClick={()=>session?logout():setLogin(true)}><span className="avatar"><Icon name="user"/></span><span>{session?`${session.username} · 로그아웃`:'관리자 로그인'}</span></button></div>
    </header>
    {notice&&<div className="global-notice" role="status">{notice}<button onClick={()=>setNotice('')} aria-label="알림 닫기">×</button></div>}
    <div className="workspace">
      <aside className="panel filters"><div className="panel-heading"><h2>필터</h2><button className="small-button" onClick={reset}>초기화</button></div><form onSubmit={e=>{e.preventDefault();apply(draft)}}>
        <Field label="공장 · 기업 검색"><div className="search-input"><Icon name="search" size={17}/><input value={draft.keyword} onChange={e=>setDraft({...draft,keyword:e.target.value})} placeholder="공장명, 기업명, 주소"/></div></Field>
        <Field label="지역"><select value={draft.sido} onChange={e=>setDraft({...draft,sido:e.target.value,sigungu:''})}><option value="">전체 지역</option>{regions.data?.filter(r=>r.label!=='미분류').map(r=><option key={r.label}>{r.label}</option>)}</select></Field>
        <Field label="시 · 군 · 구"><input value={draft.sigungu} onChange={e=>setDraft({...draft,sigungu:e.target.value})} placeholder="예: 송파구"/></Field>
        <Field label="업종·제품 카테고리"><select value={draft.category} onChange={e=>apply({...draft,category:e.target.value})}><option value="">전체 카테고리</option>{categories.data?.map(c=><option key={c}>{c}</option>)}</select></Field>
        <Field label="생산 제품"><input value={draft.product} onChange={e=>setDraft({...draft,product:e.target.value})} placeholder="제품명으로 검색"/></Field>
        <button className="primary block" type="submit"><Icon name="search" size={18}/>공장 검색</button>
      </form><div className="filter-divider"/><div className="panel-heading"><h3>카테고리 바로 선택</h3></div><div className="category-grid"><button aria-pressed={!filters.category} className={!filters.category?'active':''} onClick={()=>category('')}><CategoryIcon category="전체"/>전체</button>{categories.data?.map(c=><button key={c} aria-pressed={filters.category===c} className={filters.category===c?'active':''} onClick={()=>category(filters.category===c?'':c)}><CategoryIcon category={c}/>{c}</button>)}</div>{categories.error&&<p className="notice error">카테고리를 불러오지 못했습니다.</p>}
        <div className="filter-summary"><span className="eyebrow">검색 결과</span><strong>{number(factories.data?.totalElements)}<small> 곳</small></strong><p>{filters.category || '전체 카테고리'} · {filters.sido || '전국'}</p></div><div className="connection"><span className={`status-dot ${health.data?'online':''}`}/>{health.loading?'연결 확인 중':health.data?'데이터 서버 연결됨':'데이터 서버 연결 필요'}</div>
      </aside>
      <main className="main-content">
        {view==='map'?<><section className="map-panel panel"><div className="map-top"><div className="map-counter"><span>표시 중인 공장</span><strong>{number(mapCount)}<small> 곳</small></strong><p>검색 결과 {number(factories.data?.totalElements)}곳</p></div><div className="category-ribbon" aria-label="지도 카테고리"><button className={!filters.category?'active':''} onClick={()=>category('')}>전체</button>{mapCategories.map(c=><button key={c} className={filters.category===c?'active':''} onClick={()=>category(filters.category===c?'':c)}><CategoryIcon category={c}/>{c}</button>)}</div></div><FactoryMap query={query} revision={revision} selected={detail.data?.factory} onSelect={setSelectedId} onCount={setMapCount}/></section><div className="dashboard-stats"><StatsPanel title="카테고리별 공장 수" resource={categoryStats} donut onPick={c=>category(c==='미분류'?'':c)}/><StatsPanel title="지역별 공장 수 TOP 5" resource={regions} onPick={r=>apply({...filters,sido:r==='미분류'?'':r})}/><StatsPanel title="제품별 생산 공장 TOP 5" resource={productStats} onPick={p=>apply({...filters,product:p})}/></div><section className="panel map-results"><div className="panel-heading"><h3>검색된 공장</h3><button className="text-button" onClick={()=>setView('factories')}>목록 전체 보기 <Icon name="arrow" size={16}/></button></div><State {...factories} empty={!factories.data?.content.length}><div className="compact-list">{factories.data?.content.slice(0,4).map(f=><button key={f.factoryId} onClick={()=>setSelectedId(f.factoryId)}><Icon/><span><strong>{f.factoryName}</strong><small>{f.address||'주소 미등록'}</small></span><span className="tag">{f.latitude==null?'좌표 미등록':'위치 확인'}</span></button>)}</div></State></section></>
        :view==='factories'?<section className="panel list-view"><div className="view-heading"><div><span className="eyebrow">FACTORY DIRECTORY</span><h2>공장 목록 <span>{number(factories.data?.totalElements)}</span></h2><p className="muted">{filters.category||'전체 카테고리'} · {filters.sido||'전체 지역'}</p></div>{token&&<button className="primary" onClick={()=>setEditor({kind:'factories'})}><Icon name="plus"/>공장 등록</button>}</div><State {...factories} empty={!factories.data?.content.length}><div className="table-wrap"><table><thead><tr><th>공장명 / 기업</th><th>지역 / 주소</th><th>업종</th><th>위치</th></tr></thead><tbody>{factories.data?.content.map(f=><tr key={f.factoryId} className={selectedId===f.factoryId?'selected':''}><td><button className="text-button" onClick={()=>setSelectedId(f.factoryId)}>{f.factoryName}</button><small className="cell-description">{f.companyName||'기업 미등록'}</small></td><td>{f.address||'미등록'}</td><td>{f.industry||'미등록'}</td><td><button className="small-button" onClick={()=>selectFactory(f.factoryId)}>{f.latitude==null?'정보 보기':'지도 보기'}</button></td></tr>)}</tbody></table></div></State><Pagination page={page} totalPages={factories.data?.totalPages} onChange={setPage}/></section>
        :view==='products'?<section className="panel list-view"><div className="view-heading"><div><span className="eyebrow">PRODUCT DIRECTORY</span><h2>제품 목록 <span>{number(products.data?.totalElements)}</span></h2></div>{token&&<button className="primary" onClick={()=>setEditor({kind:'products'})}><Icon name="plus"/>제품 등록</button>}</div><div className="table-toolbar"><div className="search-input"><Icon name="search"/><input value={productKeyword} aria-label="제품명 검색" onChange={e=>{setProductKeyword(e.target.value);setProductPage(0)}} placeholder="제품명을 검색하세요"/></div><span className="tag">{filters.category||'전체 카테고리'}</span></div><State {...products} empty={!products.data?.content.length}><div className="product-directory">{products.data?.content.map(p=><button key={p.productId} onClick={()=>setProductId(p.productId)}><span className="entity-icon"><Icon name="box" size={26}/></span><span className="tag">{p.category||'미분류'}</span><h3>{p.productName}</h3><p>{p.description||'제품 설명 미등록'}</p><span className="text-link">생산 공장 보기 <Icon name="arrow" size={17}/></span></button>)}</div></State><Pagination page={productPage} totalPages={products.data?.totalPages} onChange={setProductPage}/></section>
        :view==='statistics'?<><div className="view-heading"><div><span className="eyebrow">INDUSTRY OVERVIEW</span><h2>공장 · 제품 통계</h2><p className="muted">현재 DB에 등록된 전체 데이터 기준입니다.</p></div></div><div className="metric-grid"><section className="panel metric"><Icon/><span>등록 공장</span><strong>{number(totalFactories.data?.totalElements)}<small> 곳</small></strong></section><section className="panel metric"><Icon name="box"/><span>등록 제품</span><strong>{number(totalProducts.data?.totalElements)}<small> 개</small></strong></section><section className="panel metric"><Icon name="chart"/><span>공장 카테고리</span><strong>{number(categories.data?.length)}<small> 개</small></strong></section></div><div className="statistics-grid"><StatsPanel title="카테고리별 공장" resource={categoryStats} donut limit={1000}/><StatsPanel title="지역별 공장 분포" resource={regions} limit={1000}/><StatsPanel title="제품별 생산 공장 수" resource={productStats} limit={1000}/></div></>
        :token&&<Management token={token} revision={revision} onRefresh={refresh} onFactory={selectFactory} onProduct={setProductId} onCreate={kind=>setEditor({kind})} onEditFactory={editFactory} onEditProduct={editProduct} onDelete={setDeleting}/>}
      </main>
      {['map','factories'].includes(view)&&<aside className="inspector"><FactoryDetail key={selectedId||'empty'} resource={detail} onClose={()=>setSelectedId(null)} onProduct={setProductId} token={token} onEdit={editFactory} onDelete={setDeleting} onRefresh={refresh}/><div className="inspector-note"><Icon name="pin" size={16}/>좌표가 확인된 공장만 지도에 표시됩니다.</div></aside>}
    </div>
    <footer><span>FACTORYPICK <b>공장 데이터 탐색</b></span><span>{view==='map'?'Kakao Map · 카테고리 아이콘':'전국 공장 · 생산 제품 조회'}</span></footer>
    {login&&<Login onClose={()=>setLogin(false)} onLogin={loginSuccess}/>}
    {productId&&<ProductDetail id={productId} onClose={()=>setProductId(null)} onFactory={selectFactory} token={token} onEdit={editProduct} onDelete={target=>{setProductId(null);setDeleting(target)}} revision={revision}/>}
    {editor&&token&&<Editor key={`${editor.kind}-${editor.initial?.factory?.factoryId||editor.initial?.productId||'new'}`} {...editor} token={token} onClose={()=>setEditor(null)} onSaved={()=>{refresh();setNotice('데이터를 저장했습니다.')}}/>}
    {deleting&&token&&<DeleteDialog target={deleting} token={token} onClose={()=>setDeleting(null)} onDeleted={()=>{setSelectedId(null);setProductId(null);refresh();setNotice('데이터를 삭제했습니다.')}}/>}
  </div>
}
