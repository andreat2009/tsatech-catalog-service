CREATE TABLE catalog_product_image (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_catalog_product_image_product
        FOREIGN KEY (product_id) REFERENCES catalog_product(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_catalog_product_image_product ON catalog_product_image(product_id);
CREATE INDEX idx_catalog_product_image_cover ON catalog_product_image(product_id, is_cover);
CREATE UNIQUE INDEX uq_catalog_product_image_cover_per_product
    ON catalog_product_image(product_id)
    WHERE is_cover = TRUE;
