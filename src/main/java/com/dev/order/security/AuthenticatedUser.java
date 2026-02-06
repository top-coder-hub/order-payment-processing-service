package com.dev.order.security;

public record AuthenticatedUser(
        String userId,
        UserRole role
) {}
