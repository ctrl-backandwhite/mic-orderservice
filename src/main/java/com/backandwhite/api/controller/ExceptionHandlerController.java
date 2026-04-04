package com.backandwhite.api.controller;

import com.backandwhite.api.dto.OperationResponseDtoOut;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.exception.BusinessException;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.List;

import static com.backandwhite.common.exception.Message.JSON_FORMAT_ERROR;
import static com.backandwhite.common.exception.Message.VALIDATION_ERROR;

@Log4j2
@RestControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<OperationResponseDtoOut> entityNotFoundHandlerException(EntityNotFoundException ex) {
        return new ResponseEntity<>(OperationResponseDtoOut.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .details(List.of())
                .dateTime(ZonedDateTime.now())
                .build(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
public ResponseEntity<OperationResponseDtoOut> businessExceptionHandler(BusinessException ex) {
        return new ResponseEntity<>(OperationResponseDtoOut.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .details(List.of())
                .dateTime(ZonedDateTime.now())
                .build(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<OperationResponseDtoOut> httpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        return new ResponseEntity<>(OperationResponseDtoOut.builder()
                .code(JSON_FORMAT_ERROR.getCode())
                .message(JSON_FORMAT_ERROR.getDetail())
                .details(List.of())
                .dateTime(ZonedDateTime.now())
                .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnexpectedTypeException.class)
public ResponseEntity<OperationResponseDtoOut> methodUnexpectedTypeException(UnexpectedTypeException ex) {
        return new ResponseEntity<>(OperationResponseDtoOut.builder()
                .code(VALIDATION_ERROR.getCode())
                .message(VALIDATION_ERROR.getDetail())
                .details(List.of("Hay una anotación usada sobre un tipo incompatible"))
                .dateTime(ZonedDateTime.now())
                .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<OperationResponseDtoOut> methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return new ResponseEntity<>(OperationResponseDtoOut.builder()
                .code(VALIDATION_ERROR.getCode())
                .message(VALIDATION_ERROR.getDetail())
                .details(details)
                .dateTime(ZonedDateTime.now())
                .build(), HttpStatus.BAD_REQUEST);
    }
}
