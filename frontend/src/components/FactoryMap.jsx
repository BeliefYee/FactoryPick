import { useEffect, useState, useRef } from 'react'
import { loadKakaoMaps } from '../lib/kakaoMaps'
import { api, number } from '../lib/api'
import { Icon, Modal, Pagination } from './UI'
import CategoryIcon from './CategoryIcon'
import { categoryIconSource, markerCategory } from '../lib/categoryIcons'
import { clusterFactories, shouldOpenClusterList } from '../lib/mapClusters'
export default function FactoryMap({ query, revision, selected, onSelect, onCount }) {
  const container = useRef(null)
  const [map, setMap] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('지도를 불러오는 중입니다.')
  const [satellite, setSatellite] = useState(true)
  const [clusterSelection, setClusterSelection] = useState(null)
  useEffect(() => {
    let alive = true
    loadKakaoMaps().then(maps => { if (alive) setMap(new maps.Map(container.current, { center: new maps.LatLng(36.3, 127.7), level: 13, mapTypeId: maps.MapTypeId.HYBRID })) })
      .catch(e => { if (alive) setError(e.message) })
    return () => { alive = false }
  }, [])
  useEffect(() => {
    if (!map) return
    const maps = window.kakao.maps
    const activeCategory = new URLSearchParams(query).get('category') || ''
    let overlays = [], controller, timer, alive = true
    const clearOverlays = () => { overlays.forEach(overlay => overlay.setMap(null)); overlays = [] }
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
        return new maps.CustomOverlay({ map, position, content: button, clickable: true,
          xAnchor: 0.5, yAnchor: 0.5, zIndex: count > 1 ? 2 : 1 })
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
    {map && <><button className="map-type" onClick={() => { map.setMapTypeId(satellite ? window.kakao.maps.MapTypeId.ROADMAP : window.kakao.maps.MapTypeId.HYBRID); setSatellite(!satellite) }}>{satellite ? '일반 지도' : '위성 지도'}</button><div className="map-controls"><button aria-label="지도 확대" onClick={() => map.setLevel(Math.max(1, map.getLevel() - 1))}>＋</button><button aria-label="지도 축소" onClick={() => map.setLevel(Math.min(14, map.getLevel() + 1))}>−</button><button aria-label="전국 보기" onClick={() => { map.setLevel(13); map.setCenter(new window.kakao.maps.LatLng(36.3, 127.7)) }}><Icon name="map" /></button></div><div className="map-caption">{message}</div></>}
    {clusterSelection?.query === query && clusterSelection?.revision === revision && <ClusterList
      group={clusterSelection.group} activeCategory={new URLSearchParams(query).get('category') || ''}
      onClose={() => setClusterSelection(null)} onSelect={id => { setClusterSelection(null); onSelect(id) }}/>}
  </div>
}

function ClusterList({ group, activeCategory, onClose, onSelect }) {
  const [page,setPage] = useState(0)
  return <Modal title={`같은 위치 주변의 공장 ${number(group.items.length)}곳`} onClose={onClose}>
    {group.buildingCount === 1 && group.addresses[0] && <p><strong>{group.addresses[0]}</strong></p>}
    <p className="muted">같은 주소 또는 가까운 위치의 공장입니다. 목록에서 선택하면 상세 정보를 확인할 수 있습니다.</p>
    <div className="compact-list cluster-factory-list">{group.items.slice(page*20,(page+1)*20).map(item =>
      <button key={item.factoryId} onClick={() => onSelect(item.factoryId)}>
        <CategoryIcon category={markerCategory(item.categories,activeCategory)} size={34}/>
        <span><strong>{item.factoryName}</strong><small>{item.companyName || ''} · {item.categories?.join(', ') || '미분류'}</small>{item.address && <small>{item.address}</small>}</span><Icon name="arrow"/>
      </button>)}</div>
    <Pagination page={page} totalPages={Math.ceil(group.items.length/20)} onChange={setPage}/>
  </Modal>
}
