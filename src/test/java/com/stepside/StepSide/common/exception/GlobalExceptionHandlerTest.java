package com.stepside.StepSide.common.exception;

import com.stepside.StepSide.common.exception.domain.AccountLockedException;
import com.stepside.StepSide.common.exception.domain.InvalidCredentialsException;
import com.stepside.StepSide.common.exception.domain.UserAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapUserAlreadyExistsDomainExceptionToConflict() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");

        ResponseEntity<ErrorResponseDto> response = handler.handleDomainException(
                new UserAlreadyExistsException(),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("USER_ALREADY_EXISTS", response.getBody().error());
        assertEquals("El usuario ya existe.", response.getBody().message());
    }

    @Test
    void shouldMapInvalidCredentialsDomainExceptionToUnauthorized() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        ResponseEntity<ErrorResponseDto> response = handler.handleDomainException(
                new InvalidCredentialsException(),
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_CREDENTIALS", response.getBody().error());
        assertEquals("Credenciales inválidas.", response.getBody().message());
    }

    @Test
    void shouldMapAccountLockedDomainExceptionToLocked() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        ResponseEntity<ErrorResponseDto> response = handler.handleDomainException(
                new AccountLockedException(),
                request
        );

        assertEquals(HttpStatus.LOCKED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCOUNT_LOCKED", response.getBody().error());
        assertEquals("La cuenta está bloqueada.", response.getBody().message());
    }
}
