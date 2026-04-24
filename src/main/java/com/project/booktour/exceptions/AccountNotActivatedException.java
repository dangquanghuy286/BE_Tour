package com.project.booktour.exceptions;

public class AccountNotActivatedException extends CustomException {
    public AccountNotActivatedException(String message) {
        super("ACCOUNT_NOT_ACTIVATED", message);
    }
}