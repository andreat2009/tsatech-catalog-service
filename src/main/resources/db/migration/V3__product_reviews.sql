CREATE TABLE catalog_product_review (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    author_name VARCHAR(128) NOT NULL,
    author_email VARCHAR(255),
    rating INT NOT NULL,
    title VARCHAR(255),
    text TEXT,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_product_review_product
        FOREIGN KEY (product_id) REFERENCES catalog_product(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_product_review_rating
        CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX idx_product_review_product ON catalog_product_review(product_id);
CREATE INDEX idx_product_review_approved ON catalog_product_review(approved);
