package com.backandwhite.application.usecase;

import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.domain.valueobject.ReturnStatus;
import java.util.Map;

public interface ReturnUseCase {
    ReturnRequest create(ReturnRequest request);

    ReturnRequest findById(String id);

    PageResult<ReturnRequest> findAll(Map<String, Object> filters, int page, int size, String sortBy,
            boolean ascending);

    PageResult<ReturnRequest> findByUserId(String userId, int page, int size, String sortBy, boolean ascending);

    ReturnRequest updateStatus(String id, ReturnStatus newStatus);
}
