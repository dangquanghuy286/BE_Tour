package com.project.booktour.exceptions;

public class DataNotFoundException extends CustomException {
    public DataNotFoundException(String message) {
        super("DATA_NOT_FOUND", message);
    }
}