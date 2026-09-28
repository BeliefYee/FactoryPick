-- Re-runnable classification setup. Existing product categories are preserved.
CREATE TABLE IF NOT EXISTS factory_industry_category_rule (
    industry_prefix VARCHAR(3) PRIMARY KEY,
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_factory_industry_category_rule FOREIGN KEY (category_id) REFERENCES category(category_id)
);

INSERT INTO category(category_name) VALUES
    ('식품·음료'),
    ('섬유·의류·가죽'),
    ('목재·종이·가구'),
    ('인쇄·출판'),
    ('석유·화학'),
    ('의약·바이오'),
    ('고무·플라스틱'),
    ('비금속·건자재'),
    ('금속·철강'),
    ('전자·반도체'),
    ('의료·정밀기기'),
    ('전기·전력기기'),
    ('기계·장비'),
    ('자동차·부품'),
    ('조선·기타운송장비'),
    ('기타 제조'),
    ('에너지'),
    ('환경·재활용'),
    ('건설'),
    ('도소매'),
    ('운송·물류'),
    ('소프트웨어·정보서비스'),
    ('영상·문화'),
    ('통신'),
    ('부동산·임대'),
    ('연구·엔지니어링'),
    ('전문·사업지원'),
    ('교육'),
    ('기타 서비스')
ON DUPLICATE KEY UPDATE category_name=category_name;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '01',category_id FROM category WHERE category_name='식품·음료'
UNION ALL
SELECT '02',category_id FROM category WHERE category_name='식품·음료'
UNION ALL
SELECT '03',category_id FROM category WHERE category_name='식품·음료'
UNION ALL
SELECT '10',category_id FROM category WHERE category_name='식품·음료'
UNION ALL
SELECT '11',category_id FROM category WHERE category_name='식품·음료'
UNION ALL
SELECT '12',category_id FROM category WHERE category_name='식품·음료'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '13',category_id FROM category WHERE category_name='섬유·의류·가죽'
UNION ALL
SELECT '14',category_id FROM category WHERE category_name='섬유·의류·가죽'
UNION ALL
SELECT '15',category_id FROM category WHERE category_name='섬유·의류·가죽'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '16',category_id FROM category WHERE category_name='목재·종이·가구'
UNION ALL
SELECT '17',category_id FROM category WHERE category_name='목재·종이·가구'
UNION ALL
SELECT '32',category_id FROM category WHERE category_name='목재·종이·가구'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '18',category_id FROM category WHERE category_name='인쇄·출판'
UNION ALL
SELECT '581',category_id FROM category WHERE category_name='인쇄·출판'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '19',category_id FROM category WHERE category_name='석유·화학'
UNION ALL
SELECT '20',category_id FROM category WHERE category_name='석유·화학'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '21',category_id FROM category WHERE category_name='의약·바이오'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '22',category_id FROM category WHERE category_name='고무·플라스틱'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '23',category_id FROM category WHERE category_name='비금속·건자재'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '24',category_id FROM category WHERE category_name='금속·철강'
UNION ALL
SELECT '25',category_id FROM category WHERE category_name='금속·철강'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '26',category_id FROM category WHERE category_name='전자·반도체'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '27',category_id FROM category WHERE category_name='의료·정밀기기'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '28',category_id FROM category WHERE category_name='전기·전력기기'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '29',category_id FROM category WHERE category_name='기계·장비'
UNION ALL
SELECT '34',category_id FROM category WHERE category_name='기계·장비'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '30',category_id FROM category WHERE category_name='자동차·부품'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '31',category_id FROM category WHERE category_name='조선·기타운송장비'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '33',category_id FROM category WHERE category_name='기타 제조'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '35',category_id FROM category WHERE category_name='에너지'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '36',category_id FROM category WHERE category_name='환경·재활용'
UNION ALL
SELECT '37',category_id FROM category WHERE category_name='환경·재활용'
UNION ALL
SELECT '38',category_id FROM category WHERE category_name='환경·재활용'
UNION ALL
SELECT '39',category_id FROM category WHERE category_name='환경·재활용'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '41',category_id FROM category WHERE category_name='건설'
UNION ALL
SELECT '42',category_id FROM category WHERE category_name='건설'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '46',category_id FROM category WHERE category_name='도소매'
UNION ALL
SELECT '47',category_id FROM category WHERE category_name='도소매'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '49',category_id FROM category WHERE category_name='운송·물류'
UNION ALL
SELECT '50',category_id FROM category WHERE category_name='운송·물류'
UNION ALL
SELECT '51',category_id FROM category WHERE category_name='운송·물류'
UNION ALL
SELECT '52',category_id FROM category WHERE category_name='운송·물류'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '582',category_id FROM category WHERE category_name='소프트웨어·정보서비스'
UNION ALL
SELECT '62',category_id FROM category WHERE category_name='소프트웨어·정보서비스'
UNION ALL
SELECT '63',category_id FROM category WHERE category_name='소프트웨어·정보서비스'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '59',category_id FROM category WHERE category_name='영상·문화'
UNION ALL
SELECT '60',category_id FROM category WHERE category_name='영상·문화'
UNION ALL
SELECT '90',category_id FROM category WHERE category_name='영상·문화'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '61',category_id FROM category WHERE category_name='통신'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '68',category_id FROM category WHERE category_name='부동산·임대'
UNION ALL
SELECT '76',category_id FROM category WHERE category_name='부동산·임대'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '70',category_id FROM category WHERE category_name='연구·엔지니어링'
UNION ALL
SELECT '72',category_id FROM category WHERE category_name='연구·엔지니어링'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '71',category_id FROM category WHERE category_name='전문·사업지원'
UNION ALL
SELECT '73',category_id FROM category WHERE category_name='전문·사업지원'
UNION ALL
SELECT '74',category_id FROM category WHERE category_name='전문·사업지원'
UNION ALL
SELECT '75',category_id FROM category WHERE category_name='전문·사업지원'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '85',category_id FROM category WHERE category_name='교육'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

INSERT INTO factory_industry_category_rule(industry_prefix,category_id)
SELECT '55',category_id FROM category WHERE category_name='기타 서비스'
UNION ALL
SELECT '56',category_id FROM category WHERE category_name='기타 서비스'
UNION ALL
SELECT '64',category_id FROM category WHERE category_name='기타 서비스'
UNION ALL
SELECT '65',category_id FROM category WHERE category_name='기타 서비스'
UNION ALL
SELECT '66',category_id FROM category WHERE category_name='기타 서비스'
UNION ALL
SELECT '84',category_id FROM category WHERE category_name='기타 서비스'
UNION ALL
SELECT '94',category_id FROM category WHERE category_name='기타 서비스'
UNION ALL
SELECT '95',category_id FROM category WHERE category_name='기타 서비스'
UNION ALL
SELECT '96',category_id FROM category WHERE category_name='기타 서비스'
ON DUPLICATE KEY UPDATE category_id=factory_industry_category_rule.category_id;

-- Publishing (581) and software (582) have separate display categories.
-- UNION removes duplicate classifications from multiple linked products.
CREATE OR REPLACE VIEW factory_category AS
SELECT f.factory_id, ic.category_id
FROM factory f
JOIN factory_industry_category_rule ic ON ic.industry_prefix =
    CASE WHEN LEFT(TRIM(f.primary_industry_code),2)='58'
         THEN LEFT(TRIM(f.primary_industry_code),3)
         ELSE LEFT(TRIM(f.primary_industry_code),2) END
UNION
SELECT fp.factory_id, p.category_id
FROM factory_product fp JOIN product p ON p.product_id=fp.product_id
WHERE p.category_id IS NOT NULL;

