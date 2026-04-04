package com.backandwhite.domain.exception;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public enum Message {

    INVALID_STATUS_TRANSITION("OR001", "Cannot transition order from %s to %s"),
    CART_NOT_FOUND("CA001", "No active cart found for this user/session"),
    CART_EMPTY("CA002", "Cart is empty — cannot create order"),
    COUPON_EXPIRED("CO001", "Coupon has expired"),
    COUPON_EXHAUSTED("CO002", "Coupon has reached its maximum usage limit"),
    COUPON_MIN_ORDER("CO003", "Order subtotal does not meet minimum amount of %s"),
    COUPON_USER_LIMIT("CO004", "You have already used this coupon the maximum number of times"),
    COUPON_INACTIVE("CO005", "Coupon is not active"),
    COUPON_NOT_YET_VALID("CO006", "Coupon is not yet valid"),
    RETURN_WINDOW_EXPIRED("RE001", "Return window of %d days has expired"),
    RETURN_ORDER_NOT_DELIVERED("RE002", "Can only request returns for delivered orders"),
    MAX_ADDRESSES_REACHED("AD001", "Maximum number of addresses reached");

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
