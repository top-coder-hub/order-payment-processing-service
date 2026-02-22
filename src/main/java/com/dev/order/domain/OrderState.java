package com.dev.order.domain;

import java.util.Arrays;
import java.util.Optional;

public enum OrderState {
    CREATED, PAID, CANCELLED, SHIPPED;
    /**
     * Safely converts a string to an OrderState.
     * Unlike valueOf(), this is case-insensitive and returns an Optional
     * to handle invalid user input without throwing exceptions.
     */
    public static Optional<OrderState> fromString(String value) {
        return Arrays.stream(values())
                .filter(state -> state.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
