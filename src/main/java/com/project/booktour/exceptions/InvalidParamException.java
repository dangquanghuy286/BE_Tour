package com.project.booktour.exceptions;

public class InvalidParamException extends CustomException {
    public InvalidParamException(String message) {
        super("INVALID_PARAM", message);
    }
}
