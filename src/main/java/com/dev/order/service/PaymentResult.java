/**
 * Created By Lavanyaa Karthik
 * Date: 07/02/26
 * Time: 1:52 am
 */
package com.dev.order.service;

import com.dev.order.dto.PaymentResponse;

public record PaymentResult (
        PaymentResponse paymentResponse,
        boolean isNewlyCreated
) {}
