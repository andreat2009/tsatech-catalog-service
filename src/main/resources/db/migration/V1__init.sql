CREATE TABLE catalog_manufacturer (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    image VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE catalog_category (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES catalog_category(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_catalog_category_parent ON catalog_category(parent_id);

CREATE TABLE catalog_product (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(64) UNIQUE,
    model VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(15,4) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    image VARCHAR(255),
    manufacturer_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_product_manufacturer
        FOREIGN KEY (manufacturer_id) REFERENCES catalog_manufacturer(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_catalog_product_sku ON catalog_product(sku);

CREATE TABLE catalog_product_category (
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_product_category_product
        FOREIGN KEY (product_id) REFERENCES catalog_product(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_product_category_category
        FOREIGN KEY (category_id) REFERENCES catalog_category(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_catalog_product_category_category ON catalog_product_category(category_id);
