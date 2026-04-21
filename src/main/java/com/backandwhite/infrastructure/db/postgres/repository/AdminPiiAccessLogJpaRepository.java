package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.AdminPiiAccessLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminPiiAccessLogJpaRepository extends JpaRepository<AdminPiiAccessLogEntity, Long> {

    List<AdminPiiAccessLogEntity> findAllByCustomerIdOrderByAtDesc(String customerId);

    List<AdminPiiAccessLogEntity> findAllByAdminUserIdOrderByAtDesc(String adminUserId);
}
