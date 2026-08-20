package com.stepside.StepSide.common.exception.domain;

import org.springframework.http.HttpStatus;

public class UserAlreadyDeletedException extends DomainException {

    private static final String DEFAULT_MESSAGE = "Usuario ya eliminado.";

    public UserAlreadyDeletedException() {
        super("USER_ALREADY_DELETED", DEFAULT_MESSAGE, HttpStatus.CONFLICT);
    }
}
