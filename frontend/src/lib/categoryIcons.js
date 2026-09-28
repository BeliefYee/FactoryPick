// Shared vector artwork for map images, filter buttons and detail badges.
// All artwork and colors are local constants; category text is never injected into SVG.
export const categoryIcons = [
  { name: '식품·음료', color: '#22794c', path: 'M7 3v6m-3-6v4a3 3 0 0 0 6 0V3M7 10v11M19 3c-4 3-4 8 0 9V3Zm0 9v9' },
  { name: '섬유·의류·가죽', color: '#ab4674', path: 'm8 3-6 4 3 5 3-2v11h8V10l3 2 3-5-6-4c0 4-8 4-8 0Z' },
  { name: '목재·종이·가구', color: '#8b653b', path: 'M6 13V4h12v9M4 13h16v5H4v-5Zm2 5v3m12-3v3M9 7h6m-6 3h6' },
  { name: '인쇄·출판', color: '#a77923', path: 'M12 5C8 2 4 3 2 4v15c3-1 7-1 10 2 3-3 7-3 10-2V4c-3-1-7-2-10 1Zm0 0v16M5 8h4m-4 4h4m6-4h4m-4 4h4' },
  { name: '석유·화학', color: '#9c5c23', path: 'M9 3h6m-5 0v7l-6 9a1 1 0 0 0 1 2h14a1 1 0 0 0 1-2l-6-9V3M7 15h10M9 18h1m4-1h1' },
  { name: '의약·바이오', color: '#b74967', path: 'm8 4-4 4a5.7 5.7 0 0 0 8 8l4-4a5.7 5.7 0 0 0-8-8Zm-2 2 8 8m4 2v6m-3-3h6' },
  { name: '고무·플라스틱', color: '#277e88', path: 'M20 12a8 8 0 1 1-16 0 8 8 0 0 1 16 0Zm-4 0a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM6 6l3 3m6 6 3 3M6 18l3-3m6-6 3-3' },
  { name: '비금속·건자재', color: '#937044', path: 'M3 5h18v14H3V5Zm0 7h18M9 5v7m6 0v7' },
  { name: '금속·철강', color: '#536f9a', path: 'M4 3h16v4h-5v10h5v4H4v-4h5V7H4V3Z' },
  { name: '전자·반도체', color: '#326ed0', path: 'M6 6h12v12H6V6Zm3 3h6v6H9V9ZM9 2v4m6-4v4M9 18v4m6-4v4M2 9h4m-4 6h4m12-6h4m-4 6h4' },
  { name: '의료·정밀기기', color: '#21948e', path: 'M7 3h6v4H7V3Zm3 4v6m-3 0h6M5 21h15M14 8a7 7 0 0 1 0 13M6 17h9M8 21v-4' },
  { name: '전기·전력기기', color: '#a17c10', path: 'm13 2-9 12h7l-1 8 10-13h-7l1-7Z' },
  { name: '기계·장비', color: '#5363b9', path: 'm9 3-1 3-3 1 1 3-3 2 3 2-1 3 3 1 1 3h6l1-3 3-1-1-3 3-2-3-2 1-3-3-1-1-3H9Zm7 9a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z' },
  { name: '자동차·부품', color: '#c35d3c', path: 'm5 10 2-6h10l2 6M3 10h18v8H3v-8Zm2 8v3m14-3v3M6 14h2m8 0h2' },
  { name: '조선·기타운송장비', color: '#247c9d', path: 'M6 11V5h12v6M12 2v3M3 12l9-3 9 3-4 7H7l-4-7Zm9-3v10M2 21l4-1 4 1 4-1 4 1 4-1' },
  { name: '기타 제조', color: '#72718f', path: 'M3 21V9l6 3V7l6 3V3h5v18H3ZM7 16v2m5-2v2m5-2v2' },
  { name: '에너지', color: '#b57520', path: 'M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM12 1v3m0 16v3M1 12h3m16 0h3M4 4l2 2m12 12 2 2M4 20l2-2M18 6l2-2' },
  { name: '환경·재활용', color: '#438344', path: 'm8 6 4-4 4 7m-4-2 4 2 2-4M19 12l3 5h-8m3-3-3 3 3 3M10 19H3l4-7m-4 1 4-1 2 4' },
  { name: '건설', color: '#b36a21', path: 'M3 21h18M6 21V3h3v18M3 5h18l-4-3H6m3 7h9m0-4v9m-2 0v3h4v-3h-4' },
  { name: '도소매', color: '#a34d81', path: 'M3 9l2-6h14l2 6M3 9v3h18V9M5 12v9h14v-9M9 21v-6h6v6M8 3l-1 6m9-6 1 6' },
  { name: '운송·물류', color: '#258099', path: 'M2 5h12v12H2V5Zm12 4h4l4 5v3h-8M6 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4Zm12 0a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z' },
  { name: '소프트웨어·정보서비스', color: '#6552b7', path: 'M2 3h20v15H2V3Zm6 4-3 3 3 3m8-6 3 3-3 3m-3-6-2 6M8 22h8m-4-4v4' },
  { name: '영상·문화', color: '#ae4e8f', path: 'M3 8h13v13H3V8Zm13 5 6-3v10l-6-3M3 3h13v5H3V3Zm4 0L4 8m9-5-3 5' },
  { name: '통신', color: '#4b7ea5', path: 'M10 10a2 2 0 1 1 4 0 2 2 0 0 1-4 0Zm2 2v9m-4 0h8M7 6a6 6 0 0 0 0 8m10-8a6 6 0 0 1 0 8M4 3a10 10 0 0 0 0 14M20 3a10 10 0 0 1 0 14' },
  { name: '부동산·임대', color: '#67778f', path: 'M4 21V3h11v18H4Zm11-10h5v10h-5M7 7h1m3 0h1M7 11h1m3 0h1M7 15h1m3 0h1M9 21v-3h3v3' },
  { name: '연구·엔지니어링', color: '#388678', path: 'M9 21h6m-6-4v2h6v-2c5-3 4-12-3-12s-8 9-3 12ZM12 1v1M2 10h2m16 0h2M4 3l2 2m12 0 2-2M9 11l3 3 3-3m-3 3v5' },
  { name: '전문·사업지원', color: '#7463a5', path: 'M8 6V3h8v3M2 6h20v14H2V6Zm0 6c6 3 14 3 20 0M10 12h4v4h-4v-4Z' },
  { name: '교육', color: '#427bb7', path: 'm2 8 10-5 10 5-10 5L2 8Zm4 2v8c4 3 8 3 12 0v-8m4-2v9' },
  { name: '기타 서비스', color: '#7a7487', path: 'M12 3a4 4 0 1 1 0 8 4 4 0 0 1 0-8ZM4 21v-3a5 5 0 0 1 5-5h6a5 5 0 0 1 5 5v3H4Z' },
  { name: '가공식품', color: '#358a52', path: 'M6 3h12v4H6V3Zm1 4-2 3v11h14V10l-2-3M5 12h14m-14 5h14M9 14h6' },
  { name: '유제품', color: '#388ab2', path: 'M8 2h8v4l3 4v12H5V10l3-4V2Zm0 4h8M5 10h14m-7 0v12m0-12 4-4M7 14h2' },
  { name: '콩가공품', color: '#6c8b36', path: 'M20 4C9 1 2 9 4 17c1 5 8 5 8 0 0-4 10-5 8-13ZM8 17c-1-6 5-10 9-10' },
]

const fallback = { name: '미분류', color: '#697887', path: 'M3 21V9l6 3V7l6 3V3h5v18H3ZM7 16v2m5-2v2m5-2v2' }
const all = { name: '전체', color: '#3c759e', path: 'M3 3h7v7H3V3Zm11 0h7v7h-7V3ZM3 14h7v7H3v-7Zm11 0h7v7h-7v-7Z' }
const byName = new Map([...categoryIcons, fallback, all].map(icon => [icon.name, icon]))
const images = new Map()

export function getCategoryIcon(name) { return byName.get(name) || fallback }

export function categoryIconSource(name) {
  const icon = getCategoryIcon(name)
  if (!images.has(icon.name)) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40"><circle cx="20" cy="21" r="18" fill="#071725" opacity=".3"/><circle cx="20" cy="19" r="17" fill="${icon.color}" stroke="white" stroke-width="2"/><g transform="translate(8 7)" fill="none" stroke="white" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="${icon.path}"/></g></svg>`
    images.set(icon.name, `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`)
  }
  return images.get(icon.name)
}

export function markerCategory(categories = [], activeCategory = '') {
  if (activeCategory && categories.includes(activeCategory)) return activeCategory
  return categories.find(name => byName.has(name)) || categories[0] || '미분류'
}
