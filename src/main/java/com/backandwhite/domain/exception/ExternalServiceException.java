package com.backandwhite.domain.exception;

import com.backandwhite.common.exception.BaseException;

import java.util.List;

public class ExternalServiceException extends BaseException {

    public ExternalServiceException(String code, List<String> detail) {
        super(code, detail);
    }

    public ExternalServiceException(String code, String message) {
        super(code, message);
    }

    public ExternalServiceException(String message, String code, List<String> detail) {
        super(message, code, detail);
    }
}
