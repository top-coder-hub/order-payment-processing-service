/**
 * Created By Lavanyaa Karthik
 * Date: 09/01/26
 * Time: 11:58 pm
 */
package com.dev.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull(message = "customerId is mandatory")
        Long customerId,
        @NotNull (message = "Amount is mandatory")
        @Positive (message = "Amount must be greater than zero")
        BigDecimal totalAmount,
        @NotBlank(message = "Currency code is mandatory")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase ISO code (e.g., USD, EUR, INR)")
        String currency
){}
