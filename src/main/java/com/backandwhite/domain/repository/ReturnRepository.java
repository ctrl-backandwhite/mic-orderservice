package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.ReturnRequest;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReturnRepository {
    ReturnRequest save(ReturnRequest request);

    ReturnRequest update(ReturnRequest request);

    Optional<ReturnRequest> findById(String id);

    Page<ReturnRequest> findAll(Map<String, Object> filters, Pageable pageable);

    Page<ReturnRequest> findByUserId(String userId, Pageable pageable);
}
