# 백엔드 개발 프레임워크

## 목적과 계층

HTTP 입력, 업무 규칙, SQL, 외부 API 호출을 구분하여 API 변경이 다른 계층에 퍼지는 범위를 줄입니다. 현재 Spring Boot·Spring JDBC·MySQL을 기준으로 합니다.

```text
com.factorypick.api
  FactoryPickApplication  애플리케이션 시작점
  config/                 웹·인증·암호화 설정
  controller/             HTTP 경로·입력·응답
  dto/                    요청·응답 계약
  service/                업무 규칙·트랜잭션·외부 연동
  repository/             파라미터 SQL·DB 접근
  domain/                 도메인 데이터
  exception/              예외·공통 오류 응답
  util/                   CSV 등 독립 도구
```

일반적인 신규 기능은 Controller → Service → Repository 순서로 구성합니다. 기존 FactoryController에는 Repository 직접 호출도 있으므로 이것을 현재 모든 기능이 서비스 계층을 거친다는 뜻으로 해석하지 않습니다. 신규 기능에서는 업무 판단을 Service에 모읍니다.

애플리케이션 클래스는 하위 구성요소를 찾을 수 있는 상위 패키지에 둡니다. [Spring Boot 공식 구조 안내](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html)

| 계층 | 책임 | 산출물 |
|---|---|---|
| Controller | HTTP 입력, DTO 검증, 상태 코드 | 엔드포인트 |
| Service | 업무 검증, 존재 확인, 원자적 변경 | 업무 메서드 |
| Repository | SQL 실행, 행 매핑 | 조회·저장 메서드 |
| DTO | 공개 필드·입력 제약 | 요청·응답 타입 |
| Exception handler | 내부 예외를 안정된 공개 응답으로 변환 | ApiError |

## API 계약 작성 틀

```text
기능명 / 요구사항 ID:
Method / URL:
인증: 공개 또는 관리자 Bearer token
요청: 경로·쿼리·본문 필드, 타입, 기본값, 필수 여부
응답: 상태 코드, 필드와 null 허용 여부
오류: 입력 오류 / 미인증 / 없음 / 충돌 / 외부 연동 실패
페이지·정렬: 시작 번호, 최대 크기, 안정된 정렬 기준
DB 영향: 대상 테이블, 트랜잭션, 마이그레이션 여부
검증: 정상·경계·실패·권한 시나리오
```

현재 검색 계약 예:

```http
GET /api/factories?keyword=식품&sido=서울특별시&page=0&size=20
```

- keyword, sido, sigungu, category: 선택 조건.
- page: 0부터 시작, 0 이상. size: 기본 20, 1~500.
- 응답: content, page, size, totalElements, totalPages.
- 상세: GET /api/factories/{id}, 없으면 404.
- 지도: GET /api/factories/markers, 목록 필터와 영역 좌표 사용.
- 관리자 변경: /api/admin/**, 로그인 경로를 제외하고 Bearer 토큰 검사.

오류 응답은 기존 ApiError를 사용합니다.

```json
{
  "timestamp": "2026-10-08T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "요청값을 확인해 주세요.",
  "path": "/api/example",
  "fieldErrors": { "name": "필수 입력입니다." }
}
```

이 JSON은 형식 예시입니다. 실제 message와 fieldErrors는 예외 처리 결과를 따릅니다.

## Service 작성 예시

아래는 기존 FactoryService의 상세 조회 구조를 따른 재사용 예시입니다. 기존 메서드와 중복 추가하지 않고 새 도메인에 맞게 타입과 Repository를 바꿉니다.

```java
@Service
public class ExampleService {
    private final FactoryRepository factories;

    public ExampleService(FactoryRepository factories) {
        this.factories = factories;
    }

    public FactoryDetailResponse detail(long id) {
        var factory = factories.findById(id)
            .orElseThrow(() -> new NotFoundException("공장을 찾을 수 없습니다."));
        return new FactoryDetailResponse(factory, factories.categoriesForFactory(id));
    }
}
```

복사 시 package를 com.factorypick.api.service로 지정하고 org.springframework.stereotype.Service, FactoryRepository(repository), FactoryDetailResponse(dto), NotFoundException(exception)을 import합니다.

## DB·외부 연동 규칙

- 사용자 입력은 NamedParameterJdbcTemplate 파라미터로 전달합니다. 정렬 컬럼처럼 바인딩할 수 없는 값은 허용 목록으로 제한합니다.
- 여러 쓰기가 하나의 작업이면 Service의 트랜잭션으로 묶습니다. 예외를 삼켜 일부만 성공하지 않도록 합니다.
- 목록과 count 쿼리에 같은 필터를 사용하고 페이지 정렬에 고유 ID 기준을 포함합니다.
- DDL은 schema.sql과 필요한 migration을 함께 검토합니다. 기존 DB의 업그레이드와 신규 DB 생성 경로를 각각 확인합니다.
- 외부 API에는 타임아웃과 실패 정책을 정하고, 재시도 시 중복 데이터가 생기는지 확인합니다.
- 인증키·토큰·비밀번호를 응답이나 로그에 포함하지 않습니다.
- DB 통합 테스트는 별도 테스트 DB에서 실행합니다. 설정과 제약은 [기존 백엔드 안내](../../backend/README.md)를 따릅니다.

## 검증과 완료 기준

| 대상 | 최소 검증 |
|---|---|
| 입력 | 필수값, 길이·범위, page·size 경계 |
| 조회 | 정상, 없음, 복합 필터, count 일치 |
| 변경 | 생성·수정·삭제, 실패 시 원자성 |
| 인증 | 토큰 없음·유효·만료, 관리자 경로 보호 |
| 외부 연동 | 키 없음, 타임아웃, 잘못된 응답, 중복 수집 |
| 스키마 | 신규 생성과 기존 DB 이전 경로 |

backend에서 .\gradlew.bat test를 실행합니다. MySQL 관련 테스트는 MYSQL_TEST_URL 등 별도 환경 설정이 필요합니다. 통합 테스트가 건너뛰어졌다면 결과에 그 사실을 기록합니다.

현재 관리 토큰은 단일 서버 메모리에 저장되어 서버 재시작 시 만료됩니다. 다중 서버 운영을 계획할 때는 공유 세션 또는 별도 토큰 전략을 설계합니다. /api/admin/** 밖의 새 쓰기 API는 기존 인터셉터로 보호되는지 별도로 확인합니다.
