INSERT IGNORE INTO products (product_id, product_name, category, description) VALUES
  (1, '김치', '가공식품', '배추 및 무를 원료로 한 발효식품'),
  (2, '우유', '유제품', '살균 처리한 음용유'),
  (3, '두부', '콩가공품', '대두를 원료로 만든 식품');

INSERT IGNORE INTO factories
  (factory_id, business_number, factory_name, company_name, address, sido, sigungu, latitude, longitude, industry, established_year, factory_scale, phone)
VALUES
  (1, 'SAMPLE-001', '서울 식품공장', '팩토리픽푸드', '서울특별시 송파구 올림픽로 300', '서울특별시', '송파구', 37.5132940, 127.1001290, '식품 제조업', 2015, '중소', '02-000-0000'),
  (2, 'SAMPLE-002', '부산 유제품공장', '팩토리픽밀크', '부산광역시 해운대구 센텀중앙로 97', '부산광역시', '해운대구', 35.1730700, 129.1305000, '유제품 제조업', 2018, '중소', '051-000-0000');

INSERT IGNORE INTO factory_products (factory_id, product_id) VALUES (1, 1), (1, 3), (2, 2);
