package com.inventory.repository;

import com.inventory.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select c from Category c where lower(c.name) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(c.description, '')) like lower(concat('%', :q, '%')) order by c.id")
    List<Category> search(@Param("q") String q);
}
