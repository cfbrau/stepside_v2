package com.stepside.StepSide.common.exception.domain;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends DomainException {

    private static final String DEFAULT_MESSAGE = "El usuario ya existe.";

    public UserAlreadyExistsException() {
        super("USER_ALREADY_EXISTS", DEFAULT_MESSAGE, HttpStatus.CONFLICT);
    }
}
