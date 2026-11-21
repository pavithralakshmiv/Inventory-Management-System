package com.inventory.repository;

import com.inventory.entity.Purchase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findAllByOrderByIdDesc();

    List<Purchase> findBySupplierIdOrderByIdDesc(Long supplierId);

    boolean existsBySupplierId(Long supplierId);
}
