-- Apply ONCE to the normalized schema, with the backend stopped and a backup taken.
-- MySQL DDL commits implicitly. Do not rerun blindly if an ALTER fails.
ALTER TABLE factory
    ADD COLUMN factory_manage_no VARCHAR(30) NULL,
    MODIFY factory_name VARCHAR(255) NOT NULL,
    MODIFY company_id BIGINT NULL,
    MODIFY region_id BIGINT NULL,
    MODIFY address VARCHAR(1000) NULL,
    MODIFY latitude DECIMAL(10,7) NULL,
    MODIFY longitude DECIMAL(10,7) NULL,
    MODIFY industry_name TEXT NULL,
    MODIFY phone VARCHAR(100) NULL,
    ADD COLUMN representative_name VARCHAR(255) NULL,
    ADD COLUMN managing_agency_name VARCHAR(255) NULL,
    ADD COLUMN fax_number VARCHAR(100) NULL,
    ADD COLUMN first_registered_date DATE NULL,
    ADD COLUMN primary_industry_code VARCHAR(10) NULL,
    ADD COLUMN main_product_text TEXT NULL,
    ADD COLUMN homepage_raw TEXT NULL,
    ADD COLUMN industrial_complex_name VARCHAR(255) NULL,
    ADD COLUMN source_payload JSON NULL,
    ADD COLUMN last_synced_at DATETIME NULL,
    ADD COLUMN geocoding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN geocoded_at DATETIME NULL,
    DROP INDEX uq_factories_name_address,
    ADD CONSTRAINT uq_factory_manage_no UNIQUE (factory_manage_no),
    ADD CONSTRAINT chk_factory_coordinate_pair CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)),
    ADD CONSTRAINT chk_factory_geocoding_status CHECK (geocoding_status IN ('PENDING','RESOLVED','NOT_FOUND','REVIEW','FAILED'));

-- Preserve existing coordinates and timestamps; their acquisition time is unknown.
UPDATE factory SET geocoding_status='RESOLVED', updated_at=updated_at
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE TABLE factory_industry (
    factory_id BIGINT NOT NULL,
    industry_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (factory_id, industry_code),
    CONSTRAINT fk_factory_industry_factory FOREIGN KEY (factory_id) REFERENCES factory(factory_id) ON DELETE CASCADE,
    INDEX idx_factory_industry_code (industry_code)
);

ALTER TABLE product
    MODIFY category_id BIGINT NULL,
    ADD COLUMN category_key BIGINT GENERATED ALWAYS AS (COALESCE(category_id, 0)) STORED,
    DROP INDEX uq_products_name_category,
    ADD CONSTRAINT uq_products_name_category UNIQUE (product_name, category_key);
