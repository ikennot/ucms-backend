package com.ucms_backend.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final int status;
    private final String errorCode;

    public AppException(int status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
