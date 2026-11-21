package com.inventory.repository;

import com.inventory.entity.UserLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    List<UserLog> findTop1000ByOrderByIdDesc();

    List<UserLog> findTop8ByOrderByIdDesc();
}
