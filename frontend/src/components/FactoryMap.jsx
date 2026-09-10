import { useEffect, useState, useRef } from 'react'
import { loadKakaoMaps } from '../lib/kakaoMaps'
import { api } from '../lib/api'
import { Icon } from './UI'
export default function FactoryMap({ query, revision, selected, onSelect, onCount }) {
  const container = useRef(null)
  const [map, setMap] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('지도를 불러오는 중입니다.')
  const [satellite, setSatellite] = useState(true)
  useEffect(() => {
    let alive = true
    loadKakaoMaps().then(maps => { if (alive) setMap(new maps.Map(container.current, { center: new maps.LatLng(36.3, 127.7), level: 13, mapTypeId: maps.MapTypeId.HYBRID })) })
      .catch(e => { if (alive) setError(e.message) })
    return () => { alive = false }
  }, [])
  useEffect(() => {
    if (!map) return
    const maps = window.kakao.maps
    let markers = [], controller, timer, alive = true
    const refresh = async () => {
      controller?.abort(); controller = new AbortController()
      const signal = controller.signal
      const bounds = map.getBounds(), sw = bounds.getSouthWest(), ne = bounds.getNorthEast()
      const params = new URLSearchParams(query)
      Object.entries({ south: sw.getLat(), west: sw.getLng(), north: ne.getLat(), east: ne.getLng() }).forEach(([key, value]) => params.set(key, value))
      try {
        const data = await api(`/factories/markers?${params}`, { signal })
        if (!alive || signal.aborted) return
        markers.forEach(m => m.setMap(null))
        markers = data.map(item => {
          const marker = new maps.Marker({ map, position: new maps.LatLng(item.latitude, item.longitude), title: item.factoryName })
          maps.event.addListener(marker, 'click', () => onSelect(item.factoryId)); return marker
        })
        onCount(data.length)
        setMessage(data.length >= 10000 ? '표시 한도 10,000곳 · 지도를 확대해 주세요' : '마커를 선택하면 공장 정보를 확인할 수 있습니다.')
      } catch (e) { if (alive && !signal.aborted) { onCount(null); setMessage(e.message) } }
    }
    const idle = () => { clearTimeout(timer); timer = setTimeout(refresh, 200) }
    maps.event.addListener(map, 'idle', idle); refresh()
    return () => { alive = false; clearTimeout(timer); controller?.abort(); maps.event.removeListener(map, 'idle', idle); markers.forEach(m => m.setMap(null)) }
  }, [map, query, revision, onSelect, onCount])
  useEffect(() => {
    if (map && selected?.latitude != null && selected?.longitude != null) { map.setLevel(5); map.panTo(new window.kakao.maps.LatLng(selected.latitude, selected.longitude)) }
  }, [map, selected])
  return <div className="map-stage"><div className="map-canvas" ref={container} aria-label="전국 공장 위치 지도" />
    {error && <div className="map-unavailable"><Icon name="map" size={42} /><h3>지도를 준비하고 있습니다</h3><p>{error}</p><p className="muted">공장 목록과 카테고리 검색은 계속 이용할 수 있습니다.</p></div>}
    {map && <><button className="map-type" onClick={() => { map.setMapTypeId(satellite ? window.kakao.maps.MapTypeId.ROADMAP : window.kakao.maps.MapTypeId.HYBRID); setSatellite(!satellite) }}>{satellite ? '일반 지도' : '위성 지도'}</button><div className="map-controls"><button aria-label="지도 확대" onClick={() => map.setLevel(Math.max(1, map.getLevel() - 1))}>＋</button><button aria-label="지도 축소" onClick={() => map.setLevel(Math.min(14, map.getLevel() + 1))}>−</button><button aria-label="전국 보기" onClick={() => { map.setLevel(13); map.setCenter(new window.kakao.maps.LatLng(36.3, 127.7)) }}><Icon name="map" /></button></div><div className="map-caption">{message}</div></>}
  </div>
}
