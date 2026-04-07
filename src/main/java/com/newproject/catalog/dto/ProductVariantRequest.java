package com.newproject.catalog.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class ProductVariantRequest {
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Variant key can only contain letters, numbers, dot, underscore and dash")
    private String variantKey;
    private String sku;
    private String displayName;
    private String optionSummary;
    private String imageUrl;
    private BigDecimal priceOverride;
    @PositiveOrZero
    private Integer quantity;
    private Boolean active;
    @PositiveOrZero
    private Integer sortOrder;

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
