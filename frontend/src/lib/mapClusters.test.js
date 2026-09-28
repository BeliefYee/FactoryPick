import test from 'node:test'
import assert from 'node:assert/strict'
import { buildingAddress, clusterFactories, shouldOpenClusterList } from './mapClusters.js'

const factory = (id, x, y, categories = ['금속·철강']) => ({ factoryId: id, x, y, latitude: y, longitude: x, categories })
const project = item => ({ x: item.longitude, y: item.latitude })

test('nearby points across a cell edge cluster, while distant factories remain separate', () => {
  const groups = clusterFactories([factory(1,63,20),factory(2,65,20),factory(3,250,20)], project)
  assert.deepEqual(groups.map(g => g.items.length), [2,1])
  assert.equal(groups[0].longitude,64)
  assert.equal(groups[0].latitude,20)
})

test('zooming spreads clusters into individual icons', () => {
  const items = [factory(1,0,0),factory(2,20,0)]
  assert.equal(clusterFactories(items,project).length,1)
  assert.equal(clusterFactories(items,item=>({x:item.longitude*10,y:item.latitude*10})).length,2)
})

test('category counts count factories once and honor the selected category', () => {
  const items = [factory(1,0,0,['금속·철강','전자·반도체']),factory(2,2,0,['전자·반도체'])]
  assert.deepEqual(clusterFactories(items,project,'전자·반도체')[0].categories,[{name:'전자·반도체',count:2}])
  const mixed=clusterFactories(items,project)[0]
  assert.equal(mixed.categories.length,2)
  assert.equal(mixed.categories.reduce((sum,c)=>sum+c.count,0),2)
})

test('more than 10000 factories are retained without double counting', () => {
  const items=Array.from({length:10001},(_,id)=>factory(id,id%100,Math.floor(id/100)))
  const groups=clusterFactories(items,project)
  assert.equal(groups.reduce((sum,g)=>sum+g.items.length,0),items.length)
  assert.equal(new Set(groups.flatMap(g=>g.items.map(item=>item.factoryId))).size,items.length)
  assert.ok(groups.length<20)
  assert.deepEqual(clusterFactories([],project),[])
})

test('coincident factories or the maximum zoom open a selectable list', () => {
  const identical=clusterFactories([factory(1,10,10),factory(2,10,10)],project)[0]
  assert.equal(shouldOpenClusterList(identical,13),true)
  const nearby=clusterFactories([factory(1,10,10),factory(2,12,10)],project)[0]
  assert.equal(shouldOpenClusterList(nearby,4),false)
  assert.equal(shouldOpenClusterList(nearby,1),true)
})

test('road addresses ignore units but preserve building numbers and districts', () => {
  for (const address of [
    '서울특별시 금천구 디지털로9길 33, 5층 502호 (가산동, IT미래TOWER)',
    '서울특별시 금천구 디지털로9길 33 7층 702호',
    ' 서울특별시  금천구 디지털로9길 33 (가산동, IT 미래 TOWER) ',
  ]) assert.equal(buildingAddress(address), '서울특별시 금천구 디지털로9길 33')
  assert.equal(buildingAddress('서울특별시 금천구 디지털로9길 33-1, 101호'), '서울특별시 금천구 디지털로9길 33-1')
  assert.notEqual(buildingAddress('서울특별시 금천구 디지털로9길 33'), buildingAddress('서울특별시 구로구 디지털로9길 33'))
  assert.equal(buildingAddress('가산동 123-4 (별관)'), '가산동 123-4 (별관)')
  assert.equal(buildingAddress(null), '')
})

test('one address stays together at every zoom despite differing coordinates and units', () => {
  const items = [
    { ...factory(1,0,0), address: '서울특별시 금천구 디지털로9길 33, 502호' },
    { ...factory(2,200,0), address: '서울특별시 금천구 디지털로9길 33 7층 702호' },
  ]
  for (const scale of [1,10,100]) {
    const groups = clusterFactories(items, item => ({ x: item.longitude * scale, y: item.latitude * scale }))
    assert.equal(groups.length,1)
    assert.equal(groups[0].items.length,2)
    assert.equal(groups[0].longitude,100)
    assert.equal(groups[0].buildingCount,1)
    assert.equal(shouldOpenClusterList(groups[0],13),true)
    assert.deepEqual(groups[0].addresses,['서울특별시 금천구 디지털로9길 33'])
  }
})

test('photo addresses retain their counts when zoomed in and combine when zoomed out', () => {
  const items = [
    ...Array.from({length:131}, (_,i) => ({ ...factory(i,0,0), address:`서울특별시 금천구 디지털로9길 41, ${i+101}호` })),
    ...Array.from({length:87}, (_,i) => ({ ...factory(i+131,200,0), address:`서울특별시 금천구 디지털로9길 33, ${i+101}호` })),
  ]
  assert.deepEqual(clusterFactories(items,project).map(group => group.items.length),[131,87])
  const [group] = clusterFactories(items, item => ({ x:item.longitude / 10,y:item.latitude / 10 }))
  assert.equal(group.items.length,218)
  assert.equal(group.buildingCount,2)
  assert.equal(group.categories.reduce((sum,category)=>sum+category.count,0),218)
  assert.equal(shouldOpenClusterList(group,5),false)
})

test('clustering uses the address center and missing addresses do not combine distant factories', () => {
  const items = [
    { ...factory(1,0,0), address:'서울특별시 금천구 디지털로9길 33, 101호' },
    { ...factory(2,200,0), address:'서울특별시 금천구 디지털로9길 33, 102호' },
    { ...factory(3,110,0), address:'서울특별시 금천구 디지털로9길 33-1' },
    factory(4,400,0), factory(5,600,0),
  ]
  assert.deepEqual(clusterFactories(items,project).map(group=>group.items.length),[3,1,1])
})
