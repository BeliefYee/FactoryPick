CREATE TABLE IF NOT EXISTS company (
    company_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS region (
    region_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sido_name VARCHAR(50) NOT NULL,
    sigungu_name VARCHAR(80) NOT NULL DEFAULT '',
    CONSTRAINT uq_region_name UNIQUE (sido_name, sigungu_name)
);

CREATE TABLE IF NOT EXISTS category (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS factory (
    factory_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_number VARCHAR(30) NULL,
    factory_manage_no VARCHAR(30) NULL,
    factory_name VARCHAR(255) NOT NULL,
    company_id BIGINT NULL,
    region_id BIGINT NULL,
    address VARCHAR(1000) NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    industry_name TEXT NULL,
    representative_name VARCHAR(255) NULL,
    managing_agency_name VARCHAR(255) NULL,
    fax_number VARCHAR(100) NULL,
    first_registered_date DATE NULL,
    primary_industry_code VARCHAR(10) NULL,
    main_product_text TEXT NULL,
    homepage_raw TEXT NULL,
    industrial_complex_name VARCHAR(255) NULL,
    source_payload JSON NULL,
    last_synced_at DATETIME NULL,
    geocoding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    geocoded_at DATETIME NULL,
    employee_count INT NULL,
    established_year SMALLINT NULL,
    factory_scale VARCHAR(50) NULL,
    phone VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_factories_business_number UNIQUE (business_number),
    CONSTRAINT uq_factory_manage_no UNIQUE (factory_manage_no),
    CONSTRAINT chk_factory_coordinate_pair CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)),
    CONSTRAINT chk_factory_geocoding_status CHECK (geocoding_status IN ('PENDING','RESOLVED','NOT_FOUND','REVIEW','FAILED')),
    CONSTRAINT fk_factory_company FOREIGN KEY (company_id) REFERENCES company(company_id),
    CONSTRAINT fk_factory_region FOREIGN KEY (region_id) REFERENCES region(region_id),
    INDEX idx_factories_name (factory_name),
    INDEX idx_factories_location (latitude, longitude)
);

CREATE TABLE IF NOT EXISTS factory_industry (
    factory_id BIGINT NOT NULL,
    industry_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (factory_id, industry_code),
    CONSTRAINT fk_factory_industry_factory FOREIGN KEY (factory_id) REFERENCES factory(factory_id) ON DELETE CASCADE,
    INDEX idx_factory_industry_code (industry_code)
);

CREATE TABLE IF NOT EXISTS product (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(150) NOT NULL,
    category_id BIGINT NULL,
    category_key BIGINT GENERATED ALWAYS AS (COALESCE(category_id, 0)) STORED,
    description VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_products_name_category UNIQUE (product_name, category_key),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(category_id),
    INDEX idx_products_name (product_name)
);

CREATE TABLE IF NOT EXISTS factory_product (
    factory_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (factory_id, product_id),
    CONSTRAINT fk_factory_product_factory FOREIGN KEY (factory_id) REFERENCES factory(factory_id) ON DELETE CASCADE,
    CONSTRAINT fk_factory_product_product FOREIGN KEY (product_id) REFERENCES product(product_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS admins (
    admin_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS data_imports (
    import_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(255) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    inserted_rows INT NOT NULL DEFAULT 0,
    updated_rows INT NOT NULL DEFAULT 0,
    skipped_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NULL,
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
