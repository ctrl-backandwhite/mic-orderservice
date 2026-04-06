package com.backandwhite.domain.exception;

import com.backandwhite.common.exception.BusinessException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public enum Message {

    INVALID_STATUS_TRANSITION("OR001", "Cannot transition order from %s to %s"),
    CART_NOT_FOUND("OR002", "No active cart found for this user/session"),
    CART_EMPTY("OR003", "Cart is empty — cannot create order"),
    COUPON_EXPIRED("OR004", "Coupon has expired"),
    COUPON_EXHAUSTED("OR005", "Coupon has reached its maximum usage limit"),
    COUPON_MIN_ORDER("OR006", "Order subtotal does not meet minimum amount of %s"),
    COUPON_USER_LIMIT("OR007", "You have already used this coupon the maximum number of times"),
    COUPON_INACTIVE("OR008", "Coupon is not active"),
    COUPON_NOT_YET_VALID("OR009", "Coupon is not yet valid"),
    RETURN_WINDOW_EXPIRED("OR010", "Return window of %d days has expired"),
    RETURN_ORDER_NOT_DELIVERED("OR011", "Can only request returns for delivered orders"),
    MAX_ADDRESSES_REACHED("OR012", "Maximum number of addresses reached"),
    INSUFFICIENT_STOCK("OR013", "Insufficient stock for '%s': requested %d but only %d available"),
    PRICE_VERIFICATION_FAILED("OR014", "Unable to verify product prices — catalog service unavailable"),
    COUPON_SCOPE_MISMATCH("OR015", "Coupon does not apply to any products in your cart");

    private final String code;
    private final String detail;

    Message(String code, String detail) {
        this.code = code;
        this.detail = detail;
    }

    public String format(Object... args) {
        return String.format(this.detail, args);
    }

    public BusinessException toBusinessException(Object... args) {
        log.warn("Business rule violation: {}", format(args));
        return new BusinessException(this.code, format(args));
    }
}
