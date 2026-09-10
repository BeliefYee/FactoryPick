# 공공데이터 저장 구조

세 공공데이터 API는 같은 공장 필드를 반환하므로 하나의 factory 테이블에 저장한다.
`002-public-data-fields.sql`은 이전 정규화 DB에 최초 1회 적용한다. 신규 DB는 최신 schema.sql만 실행한다.

| 원본 필드 | DB 컬럼 |
| --- | --- |
| fctryManageNo | factory.factory_manage_no (문자열, UNIQUE) |
| cmpnyNm | factory.factory_name |
| rnAdres | factory.address |
| rprsntvNm | factory.representative_name |
| cvplChrgOrgnztNm | factory.managing_agency_name |
| cmpnyTelno / cmpnyFxnum | factory.phone / fax_number |
| allEmplyCo | factory.employee_count |
| frstFctryRegistDe | factory.first_registered_date (yyyyMMdd → DATE) |
| rprsntvIndutyCode | factory.primary_industry_code |
| indutyCodes | factory_industry.industry_code (공장별 중복 제거) |
| indutyNm | factory.industry_name |
| mainProductCn | factory.main_product_text (원문) |
| hmpadr | factory.homepage_raw (URL이 아닌 값도 보존) |
| irsttNm | factory.industrial_complex_name |
| item 전체 | factory.source_payload (JSON, item만 저장하고 인증키는 저장하지 않음) |

공장관리번호와 사업자번호, 등록일과 설립 연도, 담당 기관과 소재 지역은 서로 다른 정보다.
동일한 공장명·주소라도 관리번호가 다를 수 있으므로 이름+주소 UNIQUE는 제거했다.
회사·지역은 확인 후 연결한다. API에서 확인되지 않은 기존 값은 수집 시 유지해야 한다.
제품 분류가 없으면 category_id=NULL로 저장할 수 있으며 category_key 생성 컬럼이 미분류 제품의 중복도 방지한다.

좌표가 없으면 latitude/longitude 모두 NULL, geocoding_status=PENDING으로 저장한다.
상태는 PENDING(대기), RESOLVED(좌표 확보), NOT_FOUND(검색 결과 없음), REVIEW(확인 필요), FAILED(호출 실패)다.
좌표 획득 시 RESOLVED와 geocoded_at을 함께 저장하고, 주소가 바뀌면 이전 좌표를 무효화한 후 다시 조회한다.
기존 좌표는 유지하며 실제 획득 시각을 알 수 없으므로 geocoded_at은 NULL로 둔다.
좌표가 없는 공장도 검색·상세·통계에는 포함하며 지도 마커에서는 제외한다.

이번 변경은 DB 구조·조회 매핑·기존 입력 경로의 미확정 값 지원이다.
외부 API 자동 호출, 관리번호 기반 수집 갱신, 제품 원문 정리, 주소→좌표 API 연결은 별도 구현 대상이다.
공장관리번호로 조회하는 FactoryRepository.findByManageNo()는 준비되어 있다.
