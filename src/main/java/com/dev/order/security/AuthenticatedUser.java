package com.dev.order.security;

public record AuthenticatedUser(
        Long userId,
        UserRole role
) {}
