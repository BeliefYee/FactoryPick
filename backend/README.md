# FactoryPick Backend

전국 식품 생산 공장과 생산제품을 지도·검색·통계 화면에 제공하는 REST API 서버입니다.

## 기술 구성

- Java 17
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

### 로컬 MySQL

MySQL에서 데이터베이스를 한 번 생성합니다.

```sql
CREATE DATABASE factorypick CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

기본 접속 정보는 `root / 1234`입니다. 다르면 환경변수를 지정하거나 `src/main/resources/application.yml`을 수정합니다.

MySQL 서비스를 먼저 실행합니다. Windows에서는 서비스 목록의 `MySQL80` 상태가 실행 중인지 확인합니다.

### 여러 PC에서 공유하는 MySQL

같은 사설 네트워크의 한 PC에 MySQL Server를 설치하고, 다른 PC에서 실행하는 백엔드도 해당 MySQL을 바라보게 설정합니다. 이 프로젝트는 이미 MySQL JDBC 드라이버를 사용하므로 Docker 설정이나 별도 Java 코드 변경은 필요하지 않습니다.

MySQL 서버에서 DB와 앱 계정을 만듭니다. 아래 계정의 호스트 부분은 백엔드를 실행하는 PC의 사설 IP로 바꾸고, 여러 PC가 접속하면 PC별로 계정을 생성하거나 허용할 네트워크 범위로 제한합니다.

```sql
CREATE DATABASE factorypick CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'factorypick'@'192.168.0.100' IDENTIFIED BY '강한_비밀번호';
GRANT ALL PRIVILEGES ON factorypick.* TO 'factorypick'@'192.168.0.100';
```

DB 서버 PC의 IPv4 주소가 `192.168.0.147`이면 각 백엔드 PC의 `backend/.env`를 다음처럼 설정합니다. `DB_PASSWORD`는 MySQL에서 계정을 만들 때 사용한 비밀번호와 같아야 합니다.

```properties
DB_URL=jdbc:mysql://192.168.0.147:3306/factorypick?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=factorypick
DB_PASSWORD=강한_비밀번호
```

MySQL 서버의 원격 접속을 허용하고, Windows 방화벽에서는 사설 네트워크의 백엔드 PC만 TCP 3306에 접속할 수 있게 제한합니다. 공유기 포트 포워딩으로 인터넷에 공개하지 말고, DB 서버 주소가 바뀌지 않도록 DHCP 주소 예약을 권장합니다.

현재 개발 `.env`의 `DB_PASSWORD=1234`는 약한 비밀번호입니다. 위 계정을 만들 때 같은 값을 사용하면 연결은 되지만, 공유 네트워크에서는 더 강한 값으로 바꾸세요. 백엔드 시작 시 테이블이 생성되지만 기존 PC의 데이터를 자동 복사하거나 공공데이터를 자동 수집하지는 않습니다. 기존 데이터는 백업·복원하거나 공공데이터 API로 다시 가져와야 합니다.

### DB 구조

`etc` 코드에서 확인한 구조에 맞춰 `company`, `region`, `factory`, `category`, `product`, `factory_product`를 사용합니다.
공장은 회사·지역 ID를, 제품은 카테고리 ID를 참조하며 공장과 제품은 연결 테이블로 다대다 관계를 맺습니다. 아직 확인되지 않은 연결은 NULL을 허용합니다.
기존 REST API 주소와 요청·응답 필드는 유지합니다. 이름으로 받은 회사·지역·분류는 서버에서 ID로 변환합니다.

`etc`에는 DDL이 없으므로 타입·제약조건은 이 프로젝트의 `src/main/resources/schema.sql`에서 정의했습니다.
기존 기능을 위해 `business_number`, `factory_scale`, 생성·수정 시각과 `admins`, `data_imports`는 보존합니다.
`factory.employee_count` 등 공공데이터 필드는 조회 응답에 포함하며, 현재 관리자 입력 DTO에서는 수정하지 않습니다.
지역의 시·군·구 미지정 값은 DB에 빈 문자열로 저장하고 기존 API에는 null로 반환합니다.

### 기존 DB를 사용하는 경우: 최초 1회 이전

이미 정규화된 DB를 사용 중이라면 아래 `001` 대신 `migrations/002-public-data-fields.sql`만 최초 1회 실행합니다.
`002`는 기존 정규화 스키마를 공공데이터 필드가 있는 최신 구조로 변경합니다. 새 DB에 최신 `schema.sql`을 적용했다면 실행하지 않습니다.
MySQL의 ALTER TABLE은 암묵적으로 커밋되므로 백업 후 서버를 중지한 상태에서 실행하고, 실패 시 적용된 구조를 확인한 뒤 이어서 처리해야 합니다.

서버를 중지하고 DB를 백업한 뒤, `backend` 폴더에서 아래 순서로 실행합니다.
Windows PowerShell에서도 실행할 수 있으며, mysql.exe가 PATH에 있어야 합니다.

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -u root -p factorypick < src\main\resources\schema.sql"
cmd /c "mysql --default-character-set=utf8mb4 -u root -p factorypick < migrations\001-normalize-legacy.sql"
```

첫 명령이 성공한 뒤 두 번째 명령을 실행합니다. `--force`는 사용하지 않습니다.
배치 실행 중 SQL 오류가 나면 연결이 종료되어 미완료 트랜잭션은 롤백됩니다.
이전 대상인 새 테이블 6개가 비어 있을 때 한 번만 실행합니다.
이전 스크립트는 기존 ID·생산 관계·데이터를 복사하며 이전 테이블은 삭제하지 않습니다.
관리자와 업로드 이력은 기존 테이블을 그대로 사용합니다. 이전 완료 후 앱은 새 테이블만 사용합니다.
기존 `etc` DB가 따로 있다면 이 스크립트의 대상이 아닙니다. 먼저 컬럼과 제약조건을 schema.sql과 비교해야 합니다.

## 2. 백엔드 실행

Gradle을 별도로 설치할 필요는 없습니다. 프로젝트에 포함된 Gradle Wrapper를 사용합니다.

```powershell
.\gradlew.bat bootRun
```

```bash
./gradlew bootRun
```

처음 실행할 때 필요한 Gradle 8.14.3을 자동으로 내려받으므로 잠시 시간이 걸릴 수 있습니다.

공공데이터 수집을 사용하려면 `backend/.env`에 인증키를 설정하고 서버를 재시작합니다.
`.env.example`은 참고용이며 자동으로 읽지 않습니다.

```properties
DATA_GO_KR_SERVICE_KEY=발급받은_인증키
```

인증키가 없어도 서버는 시작됩니다. 이 상태에서 공공데이터 수집을 요청하면 HTTP 503과 인증키 설정 안내를 반환합니다.

실행 확인: `GET http://localhost:8080/api/health`

서버가 시작되면 `schema.sql`로 없는 테이블을 생성합니다. 기존 데이터 이전이나 예제 데이터 삽입은 자동 실행하지 않습니다.
새 DB에서 예제가 필요하면 `sample-data/seed.sql`을 빈 테이블에 최초 1회 실행합니다. 기존 DB 이전과 샘플 삽입 중 상황에 맞는 하나만 진행합니다.
DB 구조를 별도로 관리한다면 `.env`에 `DB_INIT_MODE=never`를 설정할 수 있습니다.

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

- `factory_name`

회사명·주소·지역·좌표·제품 카테고리는 미확정이면 비워둘 수 있습니다. 위도·경도는 둘 다 입력하거나 둘 다 비워야 합니다.

중복 판정의 기본 키는 `business_number`입니다. 같은 사업자번호의 행을 다시 올리면 기존 공장 정보를 수정합니다. 제품명과 카테고리 조합도 중복 저장되지 않습니다.

```bash
curl -X POST http://localhost:8080/api/admin/data/imports/csv \
  -H "Authorization: Bearer 발급받은토큰" \
  -F "file=@sample-data/factories.csv"
```

## 화면 연계

카카오 지도 표시와 공장 주소의 좌표 변환 설정은 [KAKAO.md](KAKAO.md)를 참고하세요.

- `SCR-01~05, SCR-11`: 공장 검색 및 마커 API
- `SCR-06~07`: 공장 상세 API
- `SCR-08~10`: 통계 API
- `SCR-12`: 제품 API
- `SCR-13~16, SCR-18`: 관리자 인증 및 CRUD API
- `SCR-17, SCR-19`: CSV 등록 및 처리 이력 API

## 테스트

`./gradlew.bat test`로 CSV 파서 테스트를 실행합니다.
MySQL 통합 테스트는 별도의 빈 테스트 DB를 만들고 `MYSQL_TEST_URL`을 지정하면 함께 실행됩니다.
예: `jdbc:mysql://localhost:3306/factorypick_normalization_test?allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul`.
접속 계정은 `MYSQL_TEST_USERNAME`, `MYSQL_TEST_PASSWORD`로 지정하며 기본값은 로컬 개발 DB와 같습니다.
실제 사용하는 DB를 지정하지 마세요. 테스트는 스키마와 관리자 계정을 생성하며, 테스트별 데이터 변경은 롤백합니다.

## 배포 전 확인사항

- 관리자 계정과 DB 비밀번호를 환경변수로 변경
- 허용할 프론트엔드 주소를 `CORS_ALLOWED_ORIGINS`에 지정
- 운영 환경에서는 `DB_INIT_MODE=never`를 사용하고 Flyway 같은 마이그레이션 도구 도입 권장
- 현재 관리자 토큰은 단일 서버 메모리에 저장되므로 서버 재시작 시 만료됨
