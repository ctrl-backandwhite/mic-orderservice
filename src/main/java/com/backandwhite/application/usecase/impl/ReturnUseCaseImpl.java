package com.backandwhite.application.usecase.impl;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;
import static com.backandwhite.domain.exception.Message.RETURN_ORDER_NOT_DELIVERED;
import static com.backandwhite.domain.exception.Message.RETURN_WINDOW_EXPIRED;

import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.application.usecase.ReturnUseCase;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.ReturnRequest;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.repository.ReturnRepository;
import com.backandwhite.domain.valueobject.OrderStatus;
import com.backandwhite.domain.valueobject.ReturnStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReturnUseCaseImpl implements ReturnUseCase {

    private static final int RETURN_WINDOW_DAYS = 30;
    private final ReturnRepository returnRepository;
    private final OrderRepository orderRepository;
    private final OrderEventPort orderEventPort;

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
        ReturnRequest saved = returnRepository.save(request);

        // Publish return requested event (L-09)
        orderEventPort.publishOrderReturnRequested(order.getId(), saved.getId(), order.getUserId(), null,
                order.getOrderNumber(), request.getReason());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequest findById(String id) {
        return returnRepository.findById(id).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ReturnRequest", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReturnRequest> findAll(Map<String, Object> filters, int page, int size, String sortBy,
            boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(returnRepository.findAll(filters, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReturnRequest> findByUserId(String userId, int page, int size, String sortBy, boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(returnRepository.findByUserId(userId, pageable));
    }

    @Override
    @Transactional
    public ReturnRequest updateStatus(String id, ReturnStatus newStatus) {
        ReturnRequest request = returnRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ReturnRequest", id));
        request.setStatus(newStatus);
        ReturnRequest updated = returnRepository.update(request);

        // Publish return approved event with refund amount (L-09/L-10)
        if (newStatus == ReturnStatus.APPROVED) {
            Order order = orderRepository.findById(request.getOrderId()).orElse(null);
            String orderRef = order != null ? order.getOrderNumber() : null;
            Money refundAmount;
            if (request.getRefundAmount() != null) {
                refundAmount = request.getRefundAmount();
            } else if (order != null) {
                refundAmount = order.getTotal();
            } else {
                refundAmount = Money.zero();
            }
            orderEventPort.publishOrderReturnApproved(request.getOrderId(), updated.getId(), request.getUserId(), null,
                    orderRef, refundAmount.toPlainString());
            log.info("Published order.return.approved for return={}, refundAmount={}", updated.getId(), refundAmount);
        }

        return updated;
    }
}
