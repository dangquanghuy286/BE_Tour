package com.project.booktour.exceptions;

public class AccountBlockedException extends CustomException {
    public AccountBlockedException(String message) {
        super("ACCOUNT_BLOCKED", message);
    }
}
