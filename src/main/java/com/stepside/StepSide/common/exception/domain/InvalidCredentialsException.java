package com.stepside.StepSide.common.exception.domain;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends DomainException {

    private static final String DEFAULT_MESSAGE = "Credenciales inválidas.";

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", DEFAULT_MESSAGE, HttpStatus.UNAUTHORIZED);
    }
}
