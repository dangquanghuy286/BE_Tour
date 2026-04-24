package com.project.booktour.exceptions;

public class PermissionDenyException extends CustomException {
    public PermissionDenyException(String message) {
        super("PERMISSION_DENIED", message);
    }
}
