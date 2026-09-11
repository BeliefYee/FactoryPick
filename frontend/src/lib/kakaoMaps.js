let loading

export function loadKakaoMaps() {
  if (window.kakao?.maps?.Map) return Promise.resolve(window.kakao.maps)
  const key = import.meta.env.VITE_KAKAO_JAVASCRIPT_KEY?.trim()
  if (!key) return Promise.reject(new Error('카카오 지도 키가 아직 설정되지 않았습니다.'))
  if (loading) return loading
  loading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    const timer = window.setTimeout(() => fail(), 15000)
    function fail() {
      clearTimeout(timer)
      script.remove()
      reject(new Error('지도를 불러오지 못했습니다. 앱 키와 등록 도메인을 확인해 주세요.'))
    }
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(key)}&autoload=false`
    script.async = true
    script.onerror = fail
    script.onload = () => {
      if (!window.kakao?.maps?.load) return fail()
      window.kakao.maps.load(() => {
        clearTimeout(timer)
        resolve(window.kakao.maps)
      })
    }
    document.head.appendChild(script)
  }).catch((error) => { loading = undefined; throw error })
  return loading
}
