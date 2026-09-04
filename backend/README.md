# FactoryPick Backend

전국 식품 생산 공장과 생산제품을 지도·검색·통계 화면에 제공하는 REST API 서버입니다.

## 기술 구성

- Java 21
- Spring Boot 3.5.7
- Spring JDBC (`NamedParameterJdbcTemplate`)
- MySQL 8
- Gradle

## 구현 범위

- 공장 위치 마커 및 지도 영역 조회
- 지역·공장·기업·제품·카테고리 복합 검색
- 공장/제품 상세 및 생산 관계 조회
- 지역·제품·카테고리 통계
- 관리자 로그인과 공장/제품 CRUD
- CSV 공공데이터 등록, 중복 방지, 기존 데이터 갱신, 처리 이력
- 입력값 검증, 공통 오류 응답, CORS 설정

마커 클러스터링과 카테고리 아이콘 렌더링은 프론트엔드 지도 라이브러리가 수행하며, 백엔드는 `/api/factories/markers`에서 좌표와 카테고리를 제공합니다.

## 1. 실행 준비

### 방법 A: 로컬 MySQL

MySQL에서 데이터베이스를 한 번 생성합니다.

```sql
CREATE DATABASE factorypick CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

기본 접속 정보는 `root / 1234`입니다. 다르면 환경변수를 지정하거나 `src/main/resources/application.yml`을 수정합니다.

### 방법 B: Docker MySQL

```bash
docker compose up -d
```

## 2. 백엔드 실행

Gradle을 별도로 설치할 필요는 없습니다. 프로젝트에 포함된 Gradle Wrapper를 사용합니다.

```powershell
.\gradlew.bat bootRun
```

```bash
./gradlew bootRun
```

처음 실행할 때 필요한 Gradle 8.14.3을 자동으로 내려받으므로 잠시 시간이 걸릴 수 있습니다.

실행 확인: `GET http://localhost:8080/api/health`

서버가 시작되면 `schema.sql`과 `data.sql`이 실행되고 예제 공장·제품 데이터가 등록됩니다.

## 관리자 로그인

개발용 기본 계정은 `admin / admin1234`입니다. 실제 배포에서는 반드시 `ADMIN_USERNAME`, `ADMIN_PASSWORD` 환경변수를 변경해야 합니다.

```http
POST /api/admin/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin1234"
}
```

응답의 `token`을 이후 관리자 요청에 사용합니다.

```http
Authorization: Bearer 발급받은토큰
```

## 주요 API

| 구분 | Method | URL | 설명 |
|---|---:|---|---|
| 지도 | GET | `/api/factories/markers?south=33&west=124&north=39&east=132` | 현재 지도 영역 마커 조회 |
| 공장 | GET | `/api/factories` | 검색 및 복합 필터 |
| 공장 | GET | `/api/factories/{id}` | 공장 및 생산제품 상세 |
| 공장 | GET | `/api/factories/{id}/products` | 공장별 생산제품 목록 |
| 제품 | GET | `/api/products` | 제품명·카테고리 검색 |
| 제품 | GET | `/api/products/{id}/factories` | 제품별 생산 공장 |
| 제품 | GET | `/api/products/categories` | 카테고리 목록 |
| 통계 | GET | `/api/statistics/regions` | 지역별 공장 수 |
| 통계 | GET | `/api/statistics/categories` | 카테고리별 공장 수 |
| 통계 | GET | `/api/statistics/products` | 제품별 공장 수 |
| 인증 | POST | `/api/admin/auth/login` | 관리자 로그인 |
| 공장관리 | POST/PUT/DELETE | `/api/admin/factories` | 공장 CRUD |
| 제품관리 | POST/PUT/DELETE | `/api/admin/products` | 제품 CRUD |
| 데이터 | POST | `/api/admin/data/imports/csv` | CSV 업로드 |
| 데이터 | GET | `/api/admin/data/imports` | 최근 처리 이력 |

### 공장 검색 예시

```http
GET /api/factories?keyword=식품&sido=서울특별시&category=가공식품&page=0&size=20
```

조건을 보내지 않으면 전체 목록을 반환합니다. `keyword`는 공장명, 기업명, 주소를 함께 검색합니다.

### 공장 등록 JSON

```json
{
  "factory": {
    "businessNumber": "FOOD-2001",
    "factoryName": "새 식품공장",
    "companyName": "새식품",
    "address": "경기도 수원시 영통구 예시로 1",
    "sido": "경기도",
    "sigungu": "수원시",
    "latitude": 37.2636,
    "longitude": 127.0286,
    "industry": "식품 제조업",
    "establishedYear": 2022,
    "factoryScale": "중소",
    "phone": "031-000-0000"
  },
  "productIds": [1, 3]
}
```

수정 시 `productIds`를 생략하면 기존 생산제품 연결을 유지하고, 빈 배열을 보내면 연결을 모두 제거합니다.

## CSV 데이터 등록

`sample-data/factories.csv`를 형식 예제로 사용할 수 있습니다. 인코딩은 UTF-8이며 필수 열은 다음과 같습니다.

- `factory_name`, `company_name`, `address`, `sido`
- `latitude`, `longitude`

중복 판정의 기본 키는 `business_number`입니다. 같은 사업자번호의 행을 다시 올리면 기존 공장 정보를 수정합니다. 제품명과 카테고리 조합도 중복 저장되지 않습니다.

```bash
curl -X POST http://localhost:8080/api/admin/data/imports/csv \
  -H "Authorization: Bearer 발급받은토큰" \
  -F "file=@sample-data/factories.csv"
```

## 화면 연계

- `SCR-01~05, SCR-11`: 공장 검색 및 마커 API
- `SCR-06~07`: 공장 상세 API
- `SCR-08~10`: 통계 API
- `SCR-12`: 제품 API
- `SCR-13~16, SCR-18`: 관리자 인증 및 CRUD API
- `SCR-17, SCR-19`: CSV 등록 및 처리 이력 API

## 배포 전 확인사항

- 관리자 계정과 DB 비밀번호를 환경변수로 변경
- 허용할 프론트엔드 주소를 `CORS_ALLOWED_ORIGINS`에 지정
- 운영 환경에서는 `DB_INIT_MODE=never`를 사용하고 Flyway 같은 마이그레이션 도구 도입 권장
- 현재 관리자 토큰은 단일 서버 메모리에 저장되므로 서버 재시작 시 만료됨
