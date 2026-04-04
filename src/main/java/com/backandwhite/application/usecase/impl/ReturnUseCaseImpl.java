package com.backandwhite.application.usecase.impl;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.util.PageableUtils;
import com.backandwhite.application.usecase.ReturnUseCase;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.repository.ReturnRepository;
import com.backandwhite.domain.valureobject.OrderStatus;
import com.backandwhite.domain.valureobject.ReturnStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;
import static com.backandwhite.domain.exception.Message.RETURN_ORDER_NOT_DELIVERED;
import static com.backandwhite.domain.exception.Message.RETURN_WINDOW_EXPIRED;

@Service
@RequiredArgsConstructor
public class ReturnUseCaseImpl implements ReturnUseCase {

    private static final int RETURN_WINDOW_DAYS = 30;
    private final ReturnRepository returnRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ReturnRequest create(ReturnRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Order", request.getOrderId()));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw RETURN_ORDER_NOT_DELIVERED.toBusinessException();
        }

        long daysSinceDelivery = Duration.between(order.getUpdatedAt(), Instant.now()).toDays();
        if (daysSinceDelivery > RETURN_WINDOW_DAYS) {
            throw RETURN_WINDOW_EXPIRED.toBusinessException(RETURN_WINDOW_DAYS);
        }

        request.setStatus(ReturnStatus.REQUESTED);
        request.setUserId(order.getUserId());
        return returnRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequest findById(String id) {
        return returnRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ReturnRequest", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationDtoOut<ReturnRequest> findAll(Map<String, Object> filters, int page, int size, String sortBy,
            boolean ascending) {
        var pageable = PageableUtils.toPageable(page, size, sortBy, ascending);
        return PageableUtils.toResponse(returnRepository.findAll(filters, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationDtoOut<ReturnRequest> findByUserId(String userId, int page, int size, String sortBy,
            boolean ascending) {
        var pageable = PageableUtils.toPageable(page, size, sortBy, ascending);
        return PageableUtils.toResponse(returnRepository.findByUserId(userId, pageable));
    }

    @Override
    @Transactional
    public ReturnRequest updateStatus(String id, ReturnStatus newStatus) {
        ReturnRequest request = returnRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ReturnRequest", id));
        request.setStatus(newStatus);
        return returnRepository.update(request);
    }
}
