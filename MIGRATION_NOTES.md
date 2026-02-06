# OpenCart -> Catalog Service mapping (initial)

This vertical focuses on products, categories, and manufacturers. The first pass keeps only core fields.

## Products
OpenCart tables:
- `oc_product`
- `oc_product_description`
- `oc_product_to_category`
- `oc_product_image`
- `oc_manufacturer`

Catalog service:
- `catalog_product`
- `catalog_product_category`
- `catalog_manufacturer`

Initial field mapping:
- `oc_product.product_id` -> `catalog_product.id`
- `oc_product.model` -> `catalog_product.model`
- `oc_product.sku` -> `catalog_product.sku`
- `oc_product.image` -> `catalog_product.image`
- `oc_product.quantity` -> `catalog_product.quantity`
- `oc_product.price` -> `catalog_product.price`
- `oc_product.status` -> `catalog_product.active`
- `oc_product.date_added` -> `catalog_product.created_at`
- `oc_product.date_modified` -> `catalog_product.updated_at`
- `oc_product.manufacturer_id` -> `catalog_product.manufacturer_id`
- `oc_product_description.name` -> `catalog_product.name`
- `oc_product_description.description` -> `catalog_product.description`
- `oc_product_to_category` -> `catalog_product_category`

## Categories
OpenCart tables:
- `oc_category`
- `oc_category_description`

Catalog service:
- `catalog_category`

Initial field mapping:
- `oc_category.category_id` -> `catalog_category.id`
- `oc_category.parent_id` -> `catalog_category.parent_id`
- `oc_category.sort_order` -> `catalog_category.sort_order`
- `oc_category.status` -> `catalog_category.active`
- `oc_category_description.name` -> `catalog_category.name`
- `oc_category_description.description` -> `catalog_category.description`

## Manufacturers
OpenCart tables:
- `oc_manufacturer`

Catalog service:
- `catalog_manufacturer`

Initial field mapping:
- `oc_manufacturer.manufacturer_id` -> `catalog_manufacturer.id`
- `oc_manufacturer.name` -> `catalog_manufacturer.name`
- `oc_manufacturer.image` -> `catalog_manufacturer.image`
- `oc_manufacturer.sort_order` -> not mapped yet
