INSERT INTO catalog_manufacturer (id, name, image, active)
VALUES
    (1001, 'TSATech Labs', '/img/manufacturers/tsatech.png', TRUE),
    (1002, 'Northwind Devices', '/img/manufacturers/northwind.png', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO catalog_category (id, parent_id, name, description, active, sort_order)
VALUES
    (1001, NULL, 'Electronics', 'Connected devices for daily work and entertainment', TRUE, 1),
    (1002, 1001, 'Wearables', 'Smart wearables and wellness trackers', TRUE, 2),
    (1003, 1001, 'Smart Home', 'Devices for home automation', TRUE, 3),
    (1004, 1001, 'Audio', 'Speakers and headphones', TRUE, 4)
ON CONFLICT (id) DO NOTHING;

INSERT INTO catalog_product (
    id,
    sku,
    model,
    name,
    description,
    price,
    quantity,
    active,
    image,
    manufacturer_id,
    created_at,
    updated_at
)
VALUES
    (1001, 'TSA-WATCH-01', 'WATCH-X1', 'TSA SmartWatch X1', 'Smartwatch with health sensors and NFC payments.', 249.9000, 35, TRUE, '/img/products/watch-x1.png', 1001, NOW(), NOW()),
    (1002, 'TSA-SPEAKER-01', 'SPEAKER-S1', 'TSA Speaker S1', 'Portable speaker with immersive stereo sound.', 129.5000, 20, TRUE, '/img/products/speaker-s1.png', 1001, NOW(), NOW()),
    (1003, 'NWD-LIGHT-01', 'LIGHT-HUB', 'Northwind Light Hub', 'Central hub for smart home lighting scenes.', 89.0000, 18, TRUE, '/img/products/light-hub.png', 1002, NOW(), NOW()),
    (1004, 'NWD-BUDS-01', 'BUDS-AIR', 'Northwind Buds Air', 'Wireless earbuds with active noise cancellation.', 159.0000, 42, TRUE, '/img/products/buds-air.png', 1002, NOW(), NOW()),
    (1005, 'TSA-CAM-01', 'CAM-GUARD', 'TSA Home Camera Guard', 'Indoor camera with night vision and AI alerts.', 99.9000, 16, TRUE, '/img/products/cam-guard.png', 1001, NOW(), NOW()),
    (1006, 'NWD-THERMO-01', 'THERMO-Z', 'Northwind Thermostat Z', 'Smart thermostat with adaptive scheduling.', 179.0000, 14, TRUE, '/img/products/thermo-z.png', 1002, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO catalog_product_category (product_id, category_id)
VALUES
    (1001, 1002),
    (1002, 1004),
    (1003, 1003),
    (1004, 1004),
    (1005, 1003),
    (1006, 1003)
ON CONFLICT DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('catalog_manufacturer', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM catalog_manufacturer),
    TRUE
);

SELECT setval(
    pg_get_serial_sequence('catalog_category', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM catalog_category),
    TRUE
);

SELECT setval(
    pg_get_serial_sequence('catalog_product', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM catalog_product),
    TRUE
);
