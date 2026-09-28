import test from 'node:test'
import assert from 'node:assert/strict'
import { categoryIconSource, getCategoryIcon, markerCategory } from './categoryIcons.js'

test('selected category determines the icon for a factory with multiple categories', () => {
  assert.equal(markerCategory(['금속·철강', '전자·반도체'], '전자·반도체'), '전자·반도체')
  assert.equal(markerCategory(['금속·철강'], '전자·반도체'), '금속·철강')
})

test('empty and custom categories still have a safe fallback icon', () => {
  assert.equal(markerCategory([]), '미분류')
  assert.equal(markerCategory(['custom']), 'custom')
  assert.equal(getCategoryIcon('custom').name, '미분류')
  assert.equal(categoryIconSource('<script>alert(1)</script>'), categoryIconSource('미분류'))
  assert.notEqual(categoryIconSource('금속·철강'), categoryIconSource('전자·반도체'))
})
