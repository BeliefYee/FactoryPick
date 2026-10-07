import test from 'node:test'
import assert from 'node:assert/strict'
import { constrainMap, MAP_AREA, viewportCorrection } from './mapBounds.js'

test('a viewport inside the area stays in place', () => {
  const result = viewportCorrection({ latitude: 36, longitude: 128 }, { south: 34, west: 126, north: 38, east: 130 })
  assert.equal(result.moved, false)
  assert.equal(result.zoomIn, false)
})

test('edges are clamped even when the map center is inside the area', () => {
  const result = viewportCorrection({ latitude: 31, longitude: 121 }, { south: 29, west: 119, north: 33, east: 123 })
  assert.equal(result.latitude, 33.5)
  assert.equal(result.longitude, 122)
  assert.equal(result.moved, true)
  const opposite = viewportCorrection({ latitude: 42, longitude: 135 }, { south: 40, west: 133, north: 44, east: 137 })
  assert.equal(opposite.latitude, 41)
  assert.equal(opposite.longitude, 134)
})

test('wide viewports must zoom in instead of bouncing between opposite edges', () => {
  assert.equal(viewportCorrection({ latitude: 36, longitude: 128 },
    { south: 29, west: 118, north: 44, east: 138 }).zoomIn, true)
})

test('the southern tile margin moves the viewport north without reducing zoom', () => {
  const bounds = { south: 30.5, west: 122, north: 37.5, east: 134 }
  const result = viewportCorrection({ latitude: 34, longitude: 128 }, bounds)
  assert.equal(result.zoomIn, false)
  assert.equal(result.latitude, 35)
  assert.equal(result.longitude, 128)
  assert.equal(bounds.south + result.latitude - 34, MAP_AREA.south)
})

class LatLng {
  constructor(latitude, longitude) { this.latitude = latitude; this.longitude = longitude }
  getLat() { return this.latitude }
  getLng() { return this.longitude }
}

test('zoom and drag corrections converge with the entire viewport in the area', () => {
  let center = new LatLng(45, 140), level = 14, maxLevel = 14
  const map = {
    getCenter: () => center, setCenter: next => { center = next },
    getLevel: () => level, setLevel: next => { level = next }, setMaxLevel: next => { maxLevel = next },
    getBounds: () => {
      const half = 2 ** (level - 11)
      return { getSouthWest: () => new LatLng(center.getLat() - half, center.getLng() - half * 2),
        getNorthEast: () => new LatLng(center.getLat() + half, center.getLng() + half * 2) }
    },
  }
  constrainMap(map, { LatLng })
  assert.equal(level, 13)
  assert.equal(maxLevel, 13)
  const bounds = map.getBounds()
  assert.ok(bounds.getSouthWest().getLat() >= MAP_AREA.south)
  assert.ok(bounds.getSouthWest().getLng() >= MAP_AREA.west)
  assert.ok(bounds.getNorthEast().getLat() <= MAP_AREA.north)
  assert.ok(bounds.getNorthEast().getLng() <= MAP_AREA.east)
  const saved = center
  constrainMap(map, { LatLng })
  assert.equal(center, saved)
})
