package com.ucms_backend.exception;

import com.ucms_backend.dto.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatus());
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            if (fieldError.getDefaultMessage() != null) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        }

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed")
                .data(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST_BODY", "Request body is missing or malformed"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("ACCESS_DENIED", "Access denied"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("FILE_TOO_LARGE", "File exceeds the maximum allowed size of 10MB"));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            Exception ex
    ) {
        String parameterName = ex instanceof MissingServletRequestParameterException parameterException
                ? parameterException.getParameterName()
                : ex instanceof MissingServletRequestPartException partException
                ? partException.getRequestPartName()
                : "file";

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR",
                        "Required parameter '" + parameterName + "' is missing"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof MissingServletRequestParameterException parameterException) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("VALIDATION_ERROR",
                                "Required parameter '" + parameterException.getParameterName() + "' is missing"));
            }
            if (current instanceof MissingServletRequestPartException partException) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("VALIDATION_ERROR",
                                "Required parameter '" + partException.getRequestPartName() + "' is missing"));
            }
            current = current.getCause();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }
}
