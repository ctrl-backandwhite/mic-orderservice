package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.db.postgres.entity.AdminPiiAccessLogEntity;
import com.backandwhite.infrastructure.db.postgres.repository.AdminPiiAccessLogJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPiiAuditService")
class AdminPiiAuditServiceTest {

    @Mock
    private AdminPiiAccessLogJpaRepository repository;

    @InjectMocks
    private AdminPiiAuditService service;

    @Test
    @DisplayName("record persists an audit row with all the fields populated")
    void recordPersists() {
        service.record("admin-1", "cust-9", "ord-1", AdminPiiAuditService.ACTION_VIEW_ADDRESS, "support ticket #42",
                "10.0.0.1");
        ArgumentCaptor<AdminPiiAccessLogEntity> captor = ArgumentCaptor.forClass(AdminPiiAccessLogEntity.class);
        verify(repository).save(captor.capture());
        AdminPiiAccessLogEntity saved = captor.getValue();
        assertThat(saved.getAdminUserId()).isEqualTo("admin-1");
        assertThat(saved.getCustomerId()).isEqualTo("cust-9");
        assertThat(saved.getOrderId()).isEqualTo("ord-1");
        assertThat(saved.getAction()).isEqualTo(AdminPiiAuditService.ACTION_VIEW_ADDRESS);
        assertThat(saved.getReason()).isEqualTo("support ticket #42");
        assertThat(saved.getSourceIp()).isEqualTo("10.0.0.1");
        assertThat(saved.getAt()).isNotNull();
    }

    @Test
    @DisplayName("listByCustomer delegates to the repository")
    void listByCustomer() {
        AdminPiiAccessLogEntity entity = AdminPiiAccessLogEntity.builder().customerId("cust-1").build();
        when(repository.findAllByCustomerIdOrderByAtDesc("cust-1")).thenReturn(List.of(entity));
        assertThat(service.listByCustomer("cust-1")).containsExactly(entity);
    }
}
