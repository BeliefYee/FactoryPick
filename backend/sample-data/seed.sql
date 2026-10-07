-- Optional sample data for an EMPTY normalized database. Run once after schema.sql.
START TRANSACTION;
INSERT INTO company(company_name) VALUES ('팩토리픽푸드'), ('팩토리픽밀크');
INSERT INTO region(sido_name,sigungu_name) VALUES ('서울특별시','송파구'), ('부산광역시','해운대구');
INSERT INTO factory(business_number,factory_name,company_id,region_id,address,latitude,longitude,industry_name,established_year,factory_scale,phone)
SELECT 'SAMPLE-001','서울 식품공장',c.company_id,r.region_id,'서울특별시 송파구 올림픽로 300',37.5132940,127.1001290,'식품 제조업',2015,'중소','02-000-0000'
FROM company c CROSS JOIN region r WHERE c.company_name='팩토리픽푸드' AND r.sido_name='서울특별시' AND r.sigungu_name='송파구';
INSERT INTO factory(business_number,factory_name,company_id,region_id,address,latitude,longitude,industry_name,established_year,factory_scale,phone)
SELECT 'SAMPLE-002','부산 유제품공장',c.company_id,r.region_id,'부산광역시 해운대구 센텀중앙로 97',35.1730700,129.1305000,'유제품 제조업',2018,'중소','051-000-0000'
FROM company c CROSS JOIN region r WHERE c.company_name='팩토리픽밀크' AND r.sido_name='부산광역시' AND r.sigungu_name='해운대구';
UPDATE factory SET geocoding_status='RESOLVED' WHERE business_number IN ('SAMPLE-001','SAMPLE-002');
UPDATE factory SET primary_industry_code='10101' WHERE business_number IN ('SAMPLE-001','SAMPLE-002');
COMMIT;
