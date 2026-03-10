CREATE TABLE catalog_product_translation (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    language_code VARCHAR(5) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT fk_catalog_product_translation_product
        FOREIGN KEY (product_id) REFERENCES catalog_product(id) ON DELETE CASCADE,
    CONSTRAINT uk_catalog_product_translation UNIQUE (product_id, language_code)
);

CREATE TABLE catalog_category_translation (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    language_code VARCHAR(5) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT fk_catalog_category_translation_category
        FOREIGN KEY (category_id) REFERENCES catalog_category(id) ON DELETE CASCADE,
    CONSTRAINT uk_catalog_category_translation UNIQUE (category_id, language_code)
);

CREATE TABLE catalog_manufacturer_translation (
    id BIGSERIAL PRIMARY KEY,
    manufacturer_id BIGINT NOT NULL,
    language_code VARCHAR(5) NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_catalog_manufacturer_translation_manufacturer
        FOREIGN KEY (manufacturer_id) REFERENCES catalog_manufacturer(id) ON DELETE CASCADE,
    CONSTRAINT uk_catalog_manufacturer_translation UNIQUE (manufacturer_id, language_code)
);

INSERT INTO catalog_product_translation (product_id, language_code, name, description)
SELECT p.id, l.language_code, p.name, p.description
FROM catalog_product p
CROSS JOIN (
    VALUES ('it'), ('en'), ('fr'), ('de'), ('es')
) AS l(language_code)
ON CONFLICT (product_id, language_code) DO NOTHING;

INSERT INTO catalog_category_translation (category_id, language_code, name, description)
SELECT c.id, l.language_code, c.name, c.description
FROM catalog_category c
CROSS JOIN (
    VALUES ('it'), ('en'), ('fr'), ('de'), ('es')
) AS l(language_code)
ON CONFLICT (category_id, language_code) DO NOTHING;

INSERT INTO catalog_manufacturer_translation (manufacturer_id, language_code, name)
SELECT m.id, l.language_code, m.name
FROM catalog_manufacturer m
CROSS JOIN (
    VALUES ('it'), ('en'), ('fr'), ('de'), ('es')
) AS l(language_code)
ON CONFLICT (manufacturer_id, language_code) DO NOTHING;
