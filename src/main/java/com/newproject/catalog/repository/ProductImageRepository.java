package com.newproject.catalog.repository;

import com.newproject.catalog.domain.ProductImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    List<ProductImage> findByProductIdAndCoverFalseOrderBySortOrderAscIdAsc(Long productId);

    Optional<ProductImage> findFirstByProductIdAndCoverTrue(Long productId);
}
