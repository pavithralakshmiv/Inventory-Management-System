package com.inventory.repository;

import com.inventory.entity.Supplier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select s from Supplier s where lower(s.name) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(s.contactPerson, '')) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(s.mobile, '')) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(s.email, '')) like lower(concat('%', :q, '%')) order by s.id")
    List<Supplier> search(@Param("q") String q);
}
