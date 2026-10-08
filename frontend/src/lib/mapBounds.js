// Keep the entire viewport in the domestic map area, including Jeju and Dokdo.
// This is an application boundary, not the provider's tile coverage contract.
// Leave a southern margin before the satellite tile edge becomes visible.
export const MAP_AREA = { south: 31.5, west: 120, north: 43, east: 136 }
// Shift the national view south so Jeju has room below its markers.
export const NATIONAL_CENTER = { latitude: 35.8, longitude: 127.7 }
export const MAX_MAP_LEVEL = 13

export function viewportCorrection(center, bounds, area = MAP_AREA) {
  if (bounds.north - bounds.south > area.north - area.south
      || bounds.east - bounds.west > area.east - area.west) return { zoomIn: true }
  const latitude = center.latitude + Math.max(0, area.south - bounds.south) - Math.max(0, bounds.north - area.north)
  const longitude = center.longitude + Math.max(0, area.west - bounds.west) - Math.max(0, bounds.east - area.east)
  return { zoomIn: false, latitude, longitude,
    moved: Math.abs(latitude - center.latitude) > 1e-7 || Math.abs(longitude - center.longitude) > 1e-7 }
}

export function constrainMap(map, maps) {
  // Projection is nonlinear, so remeasure after each correction.
  for (let attempt = 0; attempt < 12; attempt++) {
    const bounds = map.getBounds(), sw = bounds.getSouthWest(), ne = bounds.getNorthEast(), center = map.getCenter()
    const next = viewportCorrection({ latitude: center.getLat(), longitude: center.getLng() },
      { south: sw.getLat(), west: sw.getLng(), north: ne.getLat(), east: ne.getLng() })
    if (next.zoomIn) {
      if (map.getLevel() <= 1) return
      map.setLevel(map.getLevel() - 1)
      map.setMaxLevel(map.getLevel())
    } else if (next.moved) {
      map.setCenter(new maps.LatLng(next.latitude, next.longitude))
    } else return
  }
}
