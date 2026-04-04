package com.backandwhite.application.usecase;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.domain.valureobject.ReturnStatus;

import java.util.Map;

public interface ReturnUseCase {
    ReturnRequest create(ReturnRequest request);

    ReturnRequest findById(String id);

    PaginationDtoOut<ReturnRequest> findAll(Map<String, Object> filters, int page, int size, String sortBy,
            boolean ascending);

    PaginationDtoOut<ReturnRequest> findByUserId(String userId, int page, int size, String sortBy, boolean ascending);

    ReturnRequest updateStatus(String id, ReturnStatus newStatus);
}
