package com.inventory.repository;

import com.inventory.entity.StockMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findTop200ByOrderByIdDesc();

    List<StockMovement> findTop200ByProductIdOrderByIdDesc(Long productId);

    void deleteByProductId(Long productId);
}
