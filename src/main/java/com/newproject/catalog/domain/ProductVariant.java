package com.newproject.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "catalog_product_variant",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_catalog_product_variant_key", columnNames = {"product_id", "variant_key"})
    }
)
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "variant_key", nullable = false, length = 128)
    private String variantKey;

    @Column(length = 64)
    private String sku;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "option_summary", length = 1024)
    private String optionSummary;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "price_override", precision = 15, scale = 4)
    private BigDecimal priceOverride;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getVariantKey() { return variantKey; }
    public void setVariantKey(String variantKey) { this.variantKey = variantKey; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getOptionSummary() { return optionSummary; }
    public void setOptionSummary(String optionSummary) { this.optionSummary = optionSummary; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BigDecimal getPriceOverride() { return priceOverride; }
    public void setPriceOverride(BigDecimal priceOverride) { this.priceOverride = priceOverride; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
