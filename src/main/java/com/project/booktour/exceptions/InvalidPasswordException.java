package com.project.booktour.exceptions;

public class InvalidPasswordException extends CustomException {
    public InvalidPasswordException(String message) {
        super("INVALID_PASSWORD", message);
    }
}
