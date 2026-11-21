package com.inventory.repository;

import com.inventory.entity.CustomerOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findAllByOrderByIdDesc();

    List<CustomerOrder> findByCustomerIdOrderByIdDesc(Long customerId);

    List<CustomerOrder> findTop5ByOrderByIdDesc();

    boolean existsByCustomerId(Long customerId);
}
