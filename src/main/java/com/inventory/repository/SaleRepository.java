package com.inventory.repository;

import com.inventory.entity.Sale;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findAllByOrderByIdDesc();

    List<Sale> findByCustomerIdOrderByIdDesc(Long customerId);

    boolean existsByCustomerId(Long customerId);
}
