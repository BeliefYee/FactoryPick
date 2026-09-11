# 카카오 지도 연동

## 앱 설정 (사용자 계정에서 진행)

1. https://developers.kakao.com 에서 앱을 생성하거나 기존 앱을 선택합니다.
2. 앱 설정 → 앱 → 플랫폼 키에서 JavaScript 키를 확인하고 JavaScript SDK 도메인에 `http://localhost:5173`을 등록합니다. 배포 시 실제 서비스 도메인도 등록합니다.
3. 카카오맵 사용 권한/활성화가 필요한 앱은 콘솔에서 해당 설정을 완료합니다.
4. JavaScript 키와 REST API 키는 서로 다릅니다. REST API 키는 backend에서만 사용합니다.

`backend/.env`:
```properties
KAKAO_REST_API_KEY=발급받은_REST_API_키
```

`frontend/.env`:
```properties
VITE_KAKAO_JAVASCRIPT_KEY=발급받은_JavaScript_키
```

설정 후 backend와 frontend 개발 서버를 재시작합니다. 키 파일은 Git에 올리지 않습니다.
backend는 `./gradlew.bat bootRun`, frontend는 `npm run dev`로 실행합니다.
http://localhost:5173 에서 지도를 확인합니다. 개발 프록시는 /api를 localhost:8080으로 전달합니다.
운영 환경에서는 /api 역방향 프록시를 backend로 설정해야 합니다.

## 주소로 공장 좌표 저장

관리자 로그인 후 받은 Bearer 토큰으로 다음 API를 호출합니다.
```http
POST /api/admin/factories/{id}/geocode
Authorization: Bearer 관리자_토큰
```

카카오 Local 주소 검색의 `x`를 경도, `y`를 위도로 저장합니다.
주소 결과가 정확히 한 개이고 실제 주소 유형인 경우에만 저장합니다.
결과가 없으면 NOT_FOUND, 여러 결과/지역 수준 결과면 REVIEW, 외부 호출 실패면 FAILED입니다.
실패 상태는 응답의 geocodingStatus에서 확인할 수 있으며 재호출로 재시도할 수 있습니다.
키 미설정은 HTTP 503, 조회 중 주소·좌표 변경은 HTTP 409입니다.
이미 좌표가 있으면 카카오 호출 없이 반환합니다. 이 API는 관리자 권한이 필요하며 자동 일괄 실행하지 않습니다.
지도는 DB에 저장된 좌표를 기존 /api/factories/markers에서 읽습니다.
공공데이터 수집기는 별도이며, 이 연동이 공장 정보를 자동 수집하지는 않습니다.

## 공식 문서
- https://developers.kakao.com/docs/ko/local/dev-guide#address-coord
- https://apis.map.kakao.com/web/guide/
