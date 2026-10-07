import { useEffect, useLayoutEffect, useState, useRef } from 'react'
import { loadKakaoMaps } from '../lib/kakaoMaps'
import { api, number } from '../lib/api'
import { Icon, Modal, Pagination } from './UI'
import CategoryIcon from './CategoryIcon'
import { categoryIconSource, markerCategory } from '../lib/categoryIcons'
import { clusterFactories, shouldOpenClusterList } from '../lib/mapClusters'
import { constrainMap, MAX_MAP_LEVEL, NATIONAL_CENTER } from '../lib/mapBounds'
import { highlightMarker } from '../lib/mapSelection'
export default function FactoryMap({ query, revision, selected, onSelect, onCount }) {
  const container = useRef(null)
  const markerEntries = useRef([]), selectedFactoryId = useRef(null)
  const [map, setMap] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('지도를 불러오는 중입니다.')
  const [satellite, setSatellite] = useState(true)
  const [clusterSelection, setClusterSelection] = useState(null)
  useLayoutEffect(() => {
    selectedFactoryId.current = selected?.factoryId ?? null
    markerEntries.current.forEach(entry => highlightMarker(entry, selectedFactoryId.current))
  }, [selected?.factoryId])
  useEffect(() => {
    let alive = true
    loadKakaoMaps().then(maps => { if (alive) setMap(new maps.Map(container.current, { center: new maps.LatLng(NATIONAL_CENTER.latitude, NATIONAL_CENTER.longitude), level: MAX_MAP_LEVEL, mapTypeId: maps.MapTypeId.HYBRID })) })
      .catch(e => { if (alive) setError(e.message) })
    return () => { alive = false }
  }, [])
  useEffect(() => {
    if (!map) return
    const maps = window.kakao.maps
    let correcting = false
    const constrain = () => {
      if (correcting) return
      correcting = true
      try { constrainMap(map, maps) } finally { correcting = false }
    }
    const resize = () => {
      map.relayout()
      map.setMaxLevel(MAX_MAP_LEVEL)
      constrain()
    }
    map.setMinLevel(1)
    map.setMaxLevel(MAX_MAP_LEVEL)
    maps.event.addListener(map, 'bounds_changed', constrain)
    maps.event.addListener(map, 'drag', constrain)
    maps.event.addListener(map, 'dragend', constrain)
    maps.event.addListener(map, 'maptypeid_changed', constrain)
    const observer = new ResizeObserver(resize)
    observer.observe(container.current)
    constrain()
    return () => {
      observer.disconnect()
      maps.event.removeListener(map, 'bounds_changed', constrain)
      maps.event.removeListener(map, 'drag', constrain)
      maps.event.removeListener(map, 'dragend', constrain)
      maps.event.removeListener(map, 'maptypeid_changed', constrain)
    }
  }, [map])
  useEffect(() => {
    if (!map) return
    const maps = window.kakao.maps
    const activeCategory = new URLSearchParams(query).get('category') || ''
    let overlays = [], controller, timer, alive = true
    const clearOverlays = () => { overlays.forEach(overlay => overlay.setMap(null)); overlays = []; markerEntries.current = [] }
    const display = data => {
      const projection = map.getProjection()
      const groups = clusterFactories(data, item => projection.containerPointFromCoords(
        new maps.LatLng(item.latitude, item.longitude)), activeCategory)
      clearOverlays()
      overlays = groups.map(group => {
        const count = group.items.length
        const position = new maps.LatLng(group.latitude, group.longitude)
        const button = document.createElement('button')
        button.type = 'button'
        button.className = `factory-map-icon${count > 1 ? ' factory-map-cluster' : ''}`
        const label = count === 1 ? group.items[0].factoryName : `공장 ${number(count)}곳`
        const categories = group.categories.map(category => `${category.name} ${number(category.count)}곳`).join(' · ')
        const addressLabel = group.addresses.length === 1 ? group.addresses[0]
          : group.addresses.length > 1 ? `${group.addresses[0]} 외 ${number(group.addresses.length - 1)}개 주소` : ''
        button.title = [addressLabel, label, categories].filter(Boolean).join(' · ')
        button.setAttribute('aria-label', `${button.title} · ${count === 1 ? '상세 보기' : shouldOpenClusterList(group,map.getLevel()) ? '공장 목록 보기' : '확대하기'}`)
        const icons = document.createElement('span')
        icons.className = 'cluster-icons'
        group.categories.slice(0,2).forEach(category => {
          const icon = document.createElement('img')
          icon.src = categoryIconSource(category.name)
          icon.width = 40; icon.height = 40; icon.alt = ''
          icons.appendChild(icon)
        })
        button.appendChild(icons)
        if (count > 1) {
          const badge = document.createElement('span')
          badge.className = 'cluster-count'
          badge.textContent = number(count)
          button.appendChild(badge)
        }
        const selectionLabel = document.createElement('span')
        selectionLabel.className = 'marker-selection-label'
        selectionLabel.textContent = count > 1 ? '선택 회사 포함' : '선택됨'
        selectionLabel.setAttribute('aria-hidden', 'true')
        button.appendChild(selectionLabel)
        button.addEventListener('click', event => {
          event.stopPropagation()
          maps.event.preventMap()
          if (count === 1) { onSelect(group.items[0].factoryId); return }
          if (shouldOpenClusterList(group,map.getLevel())) {
            setClusterSelection({ group, query, revision })
          } else {
            map.setLevel(Math.max(1,map.getLevel()-2), { anchor: position })
            map.panTo(position)
          }
        })
        const overlay = new maps.CustomOverlay({ map, position, content: button, clickable: true,
          xAnchor: 0.5, yAnchor: 0.5, zIndex: count > 1 ? 2 : 1 })
        const entry = { overlay, button, factoryIds: new Set(group.items.map(item => item.factoryId)), defaultZIndex: count > 1 ? 2 : 1 }
        markerEntries.current.push(entry)
        highlightMarker(entry, selectedFactoryId.current)
        return overlay
      })
    }
    const refresh = async () => {
      controller?.abort(); controller = new AbortController()
      setClusterSelection(null)
      const signal = controller.signal
      const bounds = map.getBounds(), sw = bounds.getSouthWest(), ne = bounds.getNorthEast()
      const params = new URLSearchParams(query)
      Object.entries({ south: sw.getLat(), west: sw.getLng(), north: ne.getLat(), east: ne.getLng() }).forEach(([key, value]) => params.set(key, value))
      try {
        const data = await api(`/factories/markers?${params}`, { signal })
        if (!alive || signal.aborted) return
        display(data)
        onCount(data.length)
        setMessage(data.length ? '숫자는 묶인 공장 수입니다. 선택하면 확대하거나 공장 목록을 볼 수 있습니다.' : '현재 지도 영역에 해당하는 공장이 없습니다.')
      } catch (e) { if (alive && !signal.aborted) { clearOverlays(); onCount(null); setMessage(e.message) } }
    }
    const idle = () => { clearTimeout(timer); timer = setTimeout(refresh, 200) }
    maps.event.addListener(map, 'idle', idle); refresh()
    return () => { alive = false; clearTimeout(timer); controller?.abort(); maps.event.removeListener(map, 'idle', idle); clearOverlays() }
  }, [map, query, revision, onSelect, onCount])
  useEffect(() => {
    if (map && selected?.latitude != null && selected?.longitude != null) { map.setLevel(Math.min(5,map.getLevel())); map.panTo(new window.kakao.maps.LatLng(selected.latitude, selected.longitude)) }
  }, [map, selected])
  return <div className="map-stage"><div className="map-canvas" ref={container} aria-label="전국 공장 위치 지도" />
    {error && <div className="map-unavailable"><Icon name="map" size={42} /><h3>지도를 준비하고 있습니다</h3><p>{error}</p><p className="muted">공장 목록과 카테고리 검색은 계속 이용할 수 있습니다.</p></div>}
    {map && <><button className="map-type" onClick={() => { map.setMapTypeId(satellite ? window.kakao.maps.MapTypeId.ROADMAP : window.kakao.maps.MapTypeId.HYBRID); setSatellite(!satellite) }}>{satellite ? '일반 지도' : '위성 지도'}</button><div className="map-controls"><button aria-label="지도 확대" onClick={() => map.setLevel(Math.max(1, map.getLevel() - 1))}>＋</button><button aria-label="지도 축소" onClick={() => map.setLevel(Math.min(MAX_MAP_LEVEL, map.getLevel() + 1))}>−</button><button aria-label="전국 보기" onClick={() => { map.setCenter(new window.kakao.maps.LatLng(NATIONAL_CENTER.latitude, NATIONAL_CENTER.longitude)); map.setLevel(MAX_MAP_LEVEL) }}><Icon name="map" /></button></div><div className="map-caption">{message}</div></>}
    {clusterSelection?.query === query && clusterSelection?.revision === revision && <ClusterList
      group={clusterSelection.group} activeCategory={new URLSearchParams(query).get('category') || ''}
      onClose={() => setClusterSelection(null)} onSelect={id => { setClusterSelection(null); onSelect(id) }}/>}
  </div>
}

function ClusterList({ group, activeCategory, onClose, onSelect }) {
  const [page,setPage] = useState(0), [keyword,setKeyword] = useState('')
  const list = useRef(null)
  const normalize = value => (value || '').replace(/\s+/g, '').toLowerCase()
  const search = normalize(keyword)
  const matches = search ? group.items.filter(item =>
    normalize(item.factoryName).includes(search) || normalize(item.companyName).includes(search)) : group.items
  const changeKeyword = value => { setKeyword(value); setPage(0) }
  useLayoutEffect(() => {
    if (list.current) list.current.scrollTop = 0
  }, [page, keyword])
  return <Modal title={`같은 위치 주변의 공장 ${number(group.items.length)}곳`} onClose={onClose}>
    {group.buildingCount === 1 && group.addresses[0] && <p><strong>{group.addresses[0]}</strong></p>}
    <p className="muted">같은 주소 또는 가까운 위치의 공장입니다. 목록에서 선택하면 상세 정보를 확인할 수 있습니다.</p>
    <div className="search-input cluster-search"><Icon name="search" size={18}/><input
      aria-label="이 위치의 회사명·공장명 검색" placeholder="회사명 또는 공장명 검색"
      value={keyword} onChange={event => changeKeyword(event.target.value)}/>
      {keyword && <button type="button" className="icon-button" aria-label="회사 검색어 지우기" onClick={()=>changeKeyword('')}><Icon name="close" size={16}/></button>}
    </div>
    <p className="fine-print cluster-search-count" role="status">{search ? `검색 결과 ${number(matches.length)}곳 · 전체 ${number(group.items.length)}곳` : `전체 ${number(group.items.length)}곳`}</p>
    <div ref={list} className="compact-list cluster-factory-list">{matches.slice(page*20,(page+1)*20).map(item =>
      <button key={item.factoryId} onClick={() => onSelect(item.factoryId)}>
        <CategoryIcon category={markerCategory(item.categories,activeCategory)} size={34}/>
        <span><strong>{item.factoryName}</strong><small>{item.companyName || ''} · {item.categories?.join(', ') || '미분류'}</small>{item.address && <small>{item.address}</small>}</span><Icon name="arrow"/>
      </button>)}{!matches.length && <div className="empty">검색어와 일치하는 회사가 없습니다.</div>}</div>
    {!!matches.length && <Pagination page={page} totalPages={Math.ceil(matches.length/20)} onChange={setPage}/>}
  </Modal>
}
