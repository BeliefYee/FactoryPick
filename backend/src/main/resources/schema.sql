CREATE TABLE IF NOT EXISTS factories (
    factory_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_number VARCHAR(30) NULL,
    factory_name VARCHAR(150) NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    sido VARCHAR(50) NOT NULL,
    sigungu VARCHAR(80) NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    industry VARCHAR(100) NULL,
    established_year SMALLINT NULL,
    factory_scale VARCHAR(50) NULL,
    phone VARCHAR(30) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_factories_business_number UNIQUE (business_number),
    CONSTRAINT uq_factories_name_address UNIQUE (factory_name, address),
    INDEX idx_factories_region (sido, sigungu),
    INDEX idx_factories_name (factory_name),
    INDEX idx_factories_location (latitude, longitude)
);

CREATE TABLE IF NOT EXISTS products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_products_name_category UNIQUE (product_name, category),
    INDEX idx_products_category (category),
    INDEX idx_products_name (product_name)
);

CREATE TABLE IF NOT EXISTS factory_products (
    factory_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (factory_id, product_id),
    CONSTRAINT fk_factory_products_factory FOREIGN KEY (factory_id) REFERENCES factories(factory_id) ON DELETE CASCADE,
    CONSTRAINT fk_factory_products_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
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
