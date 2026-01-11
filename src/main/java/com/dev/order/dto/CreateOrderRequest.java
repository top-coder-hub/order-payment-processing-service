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
        @NotNull
        Long customerId,
        @NotNull @Positive
        BigDecimal totalAmount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$")
        String currency
){}
