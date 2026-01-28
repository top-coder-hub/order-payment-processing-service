/**
 * Created By Lavanyaa Karthik
 * Date: 10/01/26
 * Time: 12:08 am
 */
package com.dev.order.dto;

import com.dev.order.domain.OrderState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        String currency,
        OrderState orderState,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
