/**
 * Created By Lavanyaa Karthik
 * Date: 10/01/26
 * Time: 12:11 am
 */
package com.dev.order.dto;

import com.dev.order.domain.PaymentStatus;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime createdAt
) {}
