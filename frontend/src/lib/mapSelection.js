export function highlightMarker(entry, factoryId) {
  const selected = factoryId != null && entry.factoryIds.has(factoryId)
  entry.button.classList.toggle('is-selected', selected)
  entry.button.setAttribute('aria-pressed', String(selected))
  entry.overlay.setZIndex(selected ? 10 : entry.defaultZIndex)
}
