package com.inventory.repository;

import com.inventory.entity.Stock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.product.id = :productId")
    Optional<Stock> findForUpdate(@Param("productId") Long productId);

    long countByQuantityLessThanEqual(Integer quantity);

    @Query("select s.product.category.name, sum(s.quantity) from Stock s group by s.product.category.name order by s.product.category.name")
    List<Object[]> quantityByCategory();
}
