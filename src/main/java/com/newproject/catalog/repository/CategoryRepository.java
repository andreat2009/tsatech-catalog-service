package com.newproject.catalog.repository;

import com.newproject.catalog.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveOrderBySortOrderAscNameAsc(Boolean active);
}
