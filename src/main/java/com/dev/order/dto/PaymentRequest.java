/**
 * Created By Lavanyaa Karthik
 * Date: 10/01/26
 * Time: 12:01 am
 */
package com.dev.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull @Positive
        BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$")
        String currency
) {}
