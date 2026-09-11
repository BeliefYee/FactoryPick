-- Optional sample data for an EMPTY normalized database. Run once after schema.sql.
START TRANSACTION;
INSERT INTO company(company_name) VALUES ('팩토리픽푸드'), ('팩토리픽밀크');
INSERT INTO region(sido_name,sigungu_name) VALUES ('서울특별시','송파구'), ('부산광역시','해운대구');
INSERT INTO category(category_name) VALUES ('가공식품'), ('유제품'), ('콩가공품');
INSERT INTO product(product_name,category_id,description)
SELECT '김치',category_id,'배추 및 무를 원료로 한 발효식품' FROM category WHERE category_name='가공식품';
INSERT INTO product(product_name,category_id,description)
SELECT '우유',category_id,'살균 처리한 음용유' FROM category WHERE category_name='유제품';
INSERT INTO product(product_name,category_id,description)
SELECT '두부',category_id,'대두를 원료로 만든 식품' FROM category WHERE category_name='콩가공품';
INSERT INTO factory(business_number,factory_name,company_id,region_id,address,latitude,longitude,industry_name,established_year,factory_scale,phone)
SELECT 'SAMPLE-001','서울 식품공장',c.company_id,r.region_id,'서울특별시 송파구 올림픽로 300',37.5132940,127.1001290,'식품 제조업',2015,'중소','02-000-0000'
FROM company c CROSS JOIN region r WHERE c.company_name='팩토리픽푸드' AND r.sido_name='서울특별시' AND r.sigungu_name='송파구';
INSERT INTO factory(business_number,factory_name,company_id,region_id,address,latitude,longitude,industry_name,established_year,factory_scale,phone)
SELECT 'SAMPLE-002','부산 유제품공장',c.company_id,r.region_id,'부산광역시 해운대구 센텀중앙로 97',35.1730700,129.1305000,'유제품 제조업',2018,'중소','051-000-0000'
FROM company c CROSS JOIN region r WHERE c.company_name='팩토리픽밀크' AND r.sido_name='부산광역시' AND r.sigungu_name='해운대구';
INSERT INTO factory_product(factory_id,product_id)
SELECT f.factory_id,p.product_id FROM factory f CROSS JOIN product p JOIN category c ON c.category_id=p.category_id
WHERE (f.business_number='SAMPLE-001' AND ((p.product_name='김치' AND c.category_name='가공식품') OR (p.product_name='두부' AND c.category_name='콩가공품')))
   OR (f.business_number='SAMPLE-002' AND p.product_name='우유' AND c.category_name='유제품');
UPDATE factory SET geocoding_status='RESOLVED' WHERE business_number IN ('SAMPLE-001','SAMPLE-002');
COMMIT;
