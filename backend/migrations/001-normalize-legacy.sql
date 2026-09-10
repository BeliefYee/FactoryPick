-- Run schema.sql first. The six normalized tables must be EMPTY.
-- Stop the backend while migrating. Run once in mysql batch mode WITHOUT --force.
-- Legacy tables and their data are retained; no DROP or DELETE is performed.
START TRANSACTION;
INSERT INTO company(company_name) SELECT DISTINCT company_name FROM factories;
INSERT INTO region(sido_name,sigungu_name) SELECT DISTINCT sido,COALESCE(sigungu,'') FROM factories;
INSERT INTO category(category_name) SELECT DISTINCT category FROM products;
INSERT INTO factory(factory_id,business_number,factory_name,company_id,region_id,address,
    latitude,longitude,industry_name,established_year,factory_scale,phone,created_at,updated_at)
SELECT f.factory_id,f.business_number,f.factory_name,c.company_id,r.region_id,f.address,
    f.latitude,f.longitude,f.industry,f.established_year,f.factory_scale,f.phone,f.created_at,f.updated_at
FROM factories f JOIN company c ON c.company_name=f.company_name
JOIN region r ON r.sido_name=f.sido AND r.sigungu_name=COALESCE(f.sigungu,'');
INSERT INTO product(product_id,product_name,category_id,description,created_at,updated_at)
SELECT p.product_id,p.product_name,c.category_id,p.description,p.created_at,p.updated_at
FROM products p JOIN category c ON c.category_name=p.category;
INSERT INTO factory_product(factory_id,product_id) SELECT factory_id,product_id FROM factory_products;
UPDATE factory SET geocoding_status='RESOLVED', updated_at=updated_at
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
COMMIT;
