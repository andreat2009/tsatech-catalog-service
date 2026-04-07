package com.newproject.catalog.dto;

import java.math.BigDecimal;

public class ProductVariantResponse {
    private Long id;
    private String variantKey;
    private String sku;
    private String displayName;
    private String optionSummary;
    private String imageUrl;
    private BigDecimal priceOverride;
    private Integer quantity;
    private Boolean active;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
