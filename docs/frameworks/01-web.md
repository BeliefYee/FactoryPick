# 웹 개발 프레임워크

## 목적과 구조

사용자의 입력이 검색·목록·지도·상세 화면에 일관되게 반영되도록 합니다. 현재 React·Vite 구조를 유지하며 기능별 화면과 공통 도구의 책임을 구분합니다.

```text
frontend/src/
  App.jsx                 화면 전환, 공통 필터, 선택 공장, 관리자 세션
  components/
    FactoryMap.jsx        지도 SDK·마커·영역 변경
    Panels.jsx            상세·통계 패널
    Management.jsx        관리자 입력·로그인·삭제
    UI.jsx                공통 상태·모달·페이지 UI
    CategoryIcon.jsx      분류 아이콘 표시
    CategorySelect.jsx    분류 선택
  lib/
    api.js                API 호출, 쿼리 직렬화, 조회 Hook
    categoryIcons.js      공통 분류 아이콘 정의
  App.css / index.css / theme.css
```

새 기능은 먼저 기존 책임에 맞게 추가합니다. 독립된 화면이 커지면 components에 별도 파일로 분리합니다. 순수 계산·변환은 lib에, 상태·Effect를 재사용할 필요가 있을 때는 use로 시작하는 Hook에 둡니다. Hook은 컴포넌트 또는 다른 Hook의 최상위에서 호출합니다. [React 공식 Hook 안내](https://react.dev/learn/reusing-logic-with-custom-hooks)

## 데이터·상태 흐름

사용자 입력 → 입력 중인 조건(draft) → 적용된 조건(filters) → queryString → useResource → API 응답 → 화면.

| 상태 | 소유 위치 | 변경 시 규칙 |
|---|---|---|
| 입력 중인 검색어 | 검색 폼 | 제출 전 조회 조건에 반영하지 않음 |
| 적용 필터 | 공통 부모 | 목록·지도에 같은 조건 전달, page를 0으로 초기화 |
| 선택 공장 ID | 공통 부모 | 필터가 바뀌면 선택 해제 |
| API 데이터·오류 | useResource | 요청 경로가 바뀌면 이전 요청 취소 |
| 저장 결과 | 관리 화면·부모 | 성공 후 revision 증가로 조회 갱신 |
| 관리자 세션 | App.jsx | 401·만료 시 세션과 관리 화면 상태 해제 |

같은 API 결과로 계산 가능한 값을 별도 상태에 중복 저장하지 않습니다. 지도 이동·빠른 검색 시 늦게 도착한 이전 응답이 최신 결과를 덮지 않는지 확인합니다.

## 화면 작성 예시

아래는 기존 api.js와 UI.jsx를 사용하는 읽기 전용 목록 예시입니다. components/FactoryListExample.jsx로 복사할 수 있습니다. App.jsx에서 렌더링해야 표시됩니다.

```jsx
import { useState } from 'react'
import { queryString, useResource } from '../lib/api'
import { State } from './UI'

export default function FactoryListExample() {
  const [draft, setDraft] = useState('')
  const [keyword, setKeyword] = useState('')
  const query = queryString({ keyword, page: 0, size: 20 })
  const resource = useResource('/factories?' + query)
  const rows = resource.data?.content ?? []

  function search(event) {
    event.preventDefault()
    setKeyword(draft.trim())
  }

  return (
    <section aria-label="공장 검색">
      <form onSubmit={search}>
        <label htmlFor="factory-keyword">공장·기업·주소</label>
        <input id="factory-keyword" value={draft}
          onChange={event => setDraft(event.target.value)} />
        <button type="submit">검색</button>
      </form>
      <State loading={resource.loading} error={resource.error}
        empty={rows.length === 0}>
        <ul>{rows.map(row => <li key={row.factoryId}>{row.factoryName}</li>)}</ul>
      </State>
    </section>
  )
}
```

이 예시는 첫 페이지 조회에 집중합니다. 실제 목록 기능에는 페이지 이동과 전체 건수 표시를 추가하고, 필터 변경 시 page를 0으로 초기화합니다.

## API·입력 처리 규칙

- api('/factories')처럼 경로를 전달합니다. 공통 함수가 /api 접두사를 붙입니다.
- 쿼리는 queryString으로 생성하고 사용자 입력을 문자열에 직접 이어 붙이지 않습니다.
- 저장은 api(path, { method: 'POST', token, body }) 형태로 호출합니다.
- 로딩·오류·빈 결과·정상 데이터를 각각 표시합니다. 실패를 빈 목록으로 숨기지 않습니다.
- 제출 중 버튼을 비활성화하고 완료 후 성공 안내 또는 재시도 가능한 오류를 표시합니다.
- label과 입력 요소를 연결하고, 아이콘 전용 버튼에는 접근 가능한 이름을 지정합니다.
- VITE_ 환경변수는 브라우저에 노출됩니다. DB 비밀번호와 서버용 REST API 키는 백엔드 환경에 둡니다.

## 기능 작업표

| 항목 | 작성할 내용 |
|---|---|
| 사용자 행동 | 예: 지역 선택 후 검색 |
| 담당 화면·컴포넌트 | 파일명과 부모에서 받을 props |
| API 계약 | URL, 파라미터, 응답 필드, 인증 여부 |
| 상태 | 초기·로딩·빈 결과·오류·성공 |
| 변경 후 동작 | 페이지 초기화, 상세 해제, 조회 갱신 |
| 검증 | 입력 경계값, 키보드 조작, 좁은 화면, 지연 응답 |

## 완료 기준

1. 검색·목록·지도에 같은 필터가 적용됩니다.
2. 빈 결과와 서버 오류를 구분하며 키가 없는 지도도 안내를 표시합니다.
3. 관리자 기능은 로그인·로그아웃·만료 후 동작을 확인합니다.
4. 관련 단위 테스트, lint, build가 통과하고 실제 브라우저 동작을 확인합니다.

실행 환경은 [Vite 공식 안내](https://vite.dev/guide/)와 저장소 lockfile을 기준으로 준비합니다. Node.js는 Vite 요구사항을 만족하는 22.12 이상 버전을 사용합니다.
