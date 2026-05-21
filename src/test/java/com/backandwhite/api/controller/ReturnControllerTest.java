package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.in.ReturnRequestDtoIn;
import com.backandwhite.api.dto.in.UpdateReturnStatusDtoIn;
import com.backandwhite.api.dto.out.ReturnRequestDtoOut;
import com.backandwhite.api.mapper.ReturnApiMapper;
import com.backandwhite.application.usecase.ReturnUseCase;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.domain.valueobject.ReturnStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReturnControllerTest {

    @Mock
    private ReturnUseCase returnUseCase;
    @Mock
    private ReturnApiMapper returnApiMapper;

    @InjectMocks
    private ReturnController controller;

    @Test
    void createReturn_returnsCreated() {
        ReturnRequestDtoIn in = ReturnRequestDtoIn.builder().orderId("o1").reason("damaged").build();
        ReturnRequest domain = ReturnRequest.builder().build();
        ReturnRequest created = ReturnRequest.builder().id("r1").build();
        when(returnApiMapper.toDomain(in)).thenReturn(domain);
        when(returnUseCase.create(domain)).thenReturn(created);
        when(returnApiMapper.toDto(created)).thenReturn(ReturnRequestDtoOut.builder().id("r1").build());
        var resp = controller.createReturn("auth", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void getMyReturns_returnsOk() {
        PageResult<ReturnRequest> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        when(returnUseCase.findByUserId("u1", 0, 20, "createdAt", false)).thenReturn(pr);
        var resp = controller.getMyReturns("auth", "u1", 0, 20, "createdAt", false);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void findAll_buildsFilters() {
        PageResult<ReturnRequest> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        when(returnUseCase.findAll(cap.capture(), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        controller.findAll("auth", "REQUESTED", "u1", 0, 20, "createdAt", false);
        assertThat(cap.getValue()).containsEntry("status", "REQUESTED").containsEntry("userId", "u1");
    }

    @Test
    void findAll_nullParams_emptyFilters() {
        PageResult<ReturnRequest> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        when(returnUseCase.findAll(cap.capture(), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        controller.findAll("auth", null, null, 0, 20, "createdAt", false);
        assertThat(cap.getValue()).isEmpty();
    }

    @Test
    void findById_returnsOk() {
        ReturnRequest req = ReturnRequest.builder().id("r1").build();
        when(returnUseCase.findById("r1")).thenReturn(req);
        when(returnApiMapper.toDto(req)).thenReturn(ReturnRequestDtoOut.builder().build());
        var resp = controller.findById("auth", "r1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void updateStatus_returnsOk() {
        UpdateReturnStatusDtoIn dto = UpdateReturnStatusDtoIn.builder().status(ReturnStatus.APPROVED).build();
        ReturnRequest updated = ReturnRequest.builder().id("r1").build();
        when(returnUseCase.updateStatus("r1", ReturnStatus.APPROVED)).thenReturn(updated);
        when(returnApiMapper.toDto(updated)).thenReturn(ReturnRequestDtoOut.builder().build());
        var resp = controller.updateStatus("auth", "r1", dto);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }
}
