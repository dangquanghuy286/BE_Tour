package com.project.booktour.exceptions;

public class UnauthorizedException extends CustomException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super("UNAUTHORIZED", message, cause);
    }
}