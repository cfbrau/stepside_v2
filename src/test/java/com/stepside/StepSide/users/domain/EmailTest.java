package com.stepside.StepSide.users.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    void shouldNormalizeEmailToLowercaseAndTrim() {
        Email email = new Email("  User@Example.COM ");

        assertEquals("user@example.com", email.value());
    }

    @Test
    void shouldRejectInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid-email"));
    }
}
