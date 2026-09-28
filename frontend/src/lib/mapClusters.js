import { markerCategory } from './categoryIcons.js'

// Only strip units after a complete road name + building number. Keep districts,
// numbered roads and sub-numbers so neighboring addresses never share a key.
export function buildingAddress(address) {
  const original = (address || '').replace(/\s+/g, ' ').trim()
  const normalized = original.replace(/\([^)]*\)/g, ' ').replace(/\s+/g, ' ').trim()
  const road = normalized.match(/^(.+?(?:대로|로|길))\s+(\d+(?:-\d+)?)(?=$|[\s,])/u)
  return road ? `${road[1]} ${road[2]}` : original
}

// Keep each address atomic before applying screen-space buckets. Zooming must
// not split tenants of one address just because their coordinates differ.
function addressGroups(items) {
  const buildings = new Map()
  for (const item of items) {
    const address = buildingAddress(item.address)
    const key = address ? `address:${address}` : `coords:${Number(item.latitude)}:${Number(item.longitude)}`
    if (!buildings.has(key)) buildings.set(key, { address, items: [], latitude: 0, longitude: 0 })
    const building = buildings.get(key)
    building.items.push(item)
    building.latitude += Number(item.latitude)
    building.longitude += Number(item.longitude)
  }
  return [...buildings.values()].map(building => ({
    ...building,
    latitude: building.latitude / building.items.length,
    longitude: building.longitude / building.items.length,
  }))
}

// Compare neighboring buckets too, keeping work bounded for large datasets.
export function clusterFactories(items, project, activeCategory = '', radius = 64) {
  const buckets = new Map(), groups = []
  for (const building of addressGroups(items)) {
    const point = project(building)
    const cellX = Math.floor(point.x / radius), cellY = Math.floor(point.y / radius)
    let nearest = null, distance = radius * radius
    for (let x = cellX - 1; x <= cellX + 1; x++) {
      for (let y = cellY - 1; y <= cellY + 1; y++) {
        for (const group of buckets.get(`${x}:${y}`) || []) {
          const candidate = (group.x - point.x) ** 2 + (group.y - point.y) ** 2
          if (candidate <= distance) { nearest = group; distance = candidate }
        }
      }
    }
    if (!nearest) {
      nearest = { x: point.x, y: point.y, items: [], latitude: 0, longitude: 0, counts: new Map(), buildings: [] }
      const key = `${cellX}:${cellY}`
      if (!buckets.has(key)) buckets.set(key, [])
      buckets.get(key).push(nearest)
      groups.push(nearest)
    }
    nearest.buildings.push(building.address)
    for (const item of building.items) {
      nearest.items.push(item)
      nearest.latitude += Number(item.latitude)
      nearest.longitude += Number(item.longitude)
      const category = markerCategory(item.categories, activeCategory)
      nearest.counts.set(category, (nearest.counts.get(category) || 0) + 1)
    }
  }
  return groups.map(group => ({
    items: group.items,
    latitude: group.latitude / group.items.length,
    longitude: group.longitude / group.items.length,
    buildingCount: group.buildings.length,
    addresses: [...new Set(group.buildings.filter(Boolean))],
    categories: [...group.counts].map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name, 'ko')),
  }))
}

export function shouldOpenClusterList(group, level) {
  const first = group.items[0]
  return level <= 1 || group.buildingCount === 1 || group.items.every(item =>
    Number(item.latitude) === Number(first.latitude) && Number(item.longitude) === Number(first.longitude))
}
