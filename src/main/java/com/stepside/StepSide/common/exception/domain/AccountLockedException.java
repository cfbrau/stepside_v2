package com.stepside.StepSide.common.exception.domain;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends DomainException {

    private static final String DEFAULT_MESSAGE = "La cuenta está bloqueada.";

    public AccountLockedException() {
        super("ACCOUNT_LOCKED", DEFAULT_MESSAGE, HttpStatus.LOCKED);
    }
}
