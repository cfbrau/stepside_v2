package com.stepside.StepSide.common.exception.domain;

import org.springframework.http.HttpStatus;

public class UserAlreadyActiveException extends DomainException {

    private static final String DEFAULT_MESSAGE = "Usuario ya activado.";

    public UserAlreadyActiveException() {
        super("USER_ALREADY_ACTIVE", DEFAULT_MESSAGE, HttpStatus.CONFLICT);
    }
}
