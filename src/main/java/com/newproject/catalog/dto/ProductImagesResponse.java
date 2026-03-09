package com.newproject.catalog.dto;

import java.util.List;

public class ProductImagesResponse {
    private ProductImageResponse coverImage;
    private List<ProductImageResponse> galleryImages;

    public ProductImageResponse getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(ProductImageResponse coverImage) {
        this.coverImage = coverImage;
    }

    public List<ProductImageResponse> getGalleryImages() {
        return galleryImages;
    }

    public void setGalleryImages(List<ProductImageResponse> galleryImages) {
        this.galleryImages = galleryImages;
    }
}
