import test from 'node:test'
import assert from 'node:assert/strict'
import { highlightMarker } from './mapSelection.js'

function marker(ids) {
  const entry = { factoryIds: new Set(ids), defaultZIndex: ids.length > 1 ? 2 : 1 }
  entry.button = { classList: { toggle: (name, value) => { entry.selected = value } },
    setAttribute: (name, value) => { entry.pressed = value } }
  entry.overlay = { setZIndex: value => { entry.zIndex = value } }
  return entry
}

test('a selected company highlights its containing cluster above neighboring markers', () => {
  const cluster = marker([1, 2, 3]), neighbor = marker([4])
  for (const entry of [cluster, neighbor]) highlightMarker(entry, 2)
  assert.equal(cluster.selected, true)
  assert.equal(cluster.pressed, 'true')
  assert.ok(cluster.zIndex > neighbor.zIndex)
  assert.equal(neighbor.selected, false)
})

test('selection follows a company from a cluster into an individual marker after zooming', () => {
  const individual = marker([2]), remainingCluster = marker([1, 3])
  for (const entry of [individual, remainingCluster]) highlightMarker(entry, 2)
  assert.equal(individual.selected, true)
  assert.equal(remainingCluster.selected, false)
})

test('changing or clearing selection restores the previous marker', () => {
  const first = marker([1]), second = marker([2, 3])
  highlightMarker(first, 1)
  for (const entry of [first, second]) highlightMarker(entry, 3)
  assert.equal(first.selected, false)
  assert.equal(first.zIndex, 1)
  assert.equal(second.selected, true)
  highlightMarker(second, null)
  assert.equal(second.selected, false)
  assert.equal(second.pressed, 'false')
  assert.equal(second.zIndex, 2)
})
