CREATE TABLE catalog_product_variant (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variant_key VARCHAR(128) NOT NULL,
    sku VARCHAR(64),
    display_name VARCHAR(255),
    option_summary VARCHAR(1024),
    image_url VARCHAR(1024),
    price_override NUMERIC(15,4),
    quantity INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_variant_product
        FOREIGN KEY (product_id) REFERENCES catalog_product(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_catalog_product_variant_key UNIQUE (product_id, variant_key)
);

CREATE INDEX idx_catalog_product_variant_product ON catalog_product_variant(product_id);
