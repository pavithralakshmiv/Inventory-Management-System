package com.inventory.repository;

import com.inventory.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByProductCodeIgnoreCase(String productCode);

    boolean existsByProductCodeIgnoreCaseAndIdNot(String productCode, Long id);

    boolean existsByCategoryId(Long categoryId);

    boolean existsBySupplierId(Long supplierId);

    List<Product> findTop5ByOrderByIdDesc();

    @Query("select p from Product p where (lower(p.name) like lower(concat('%', :q, '%')) "
            + "or lower(p.productCode) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(p.brand, '')) like lower(concat('%', :q, '%'))) "
            + "and (:categoryId is null or p.category.id = :categoryId) order by p.id desc")
    List<Product> search(@Param("q") String q, @Param("categoryId") Long categoryId);
}
