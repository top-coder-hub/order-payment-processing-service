/**
 * Created By Lavanyaa Karthik
 * Date: 20/02/26
 * Time: 2:18 am
 */
package com.dev.order.dto;

import java.util.List;

public record PageOrderResponse (
        List<OrderResponse> content,
        int page,
        int requestedSize,
        int appliedSize,
        long totalElements,
        int totalPages,
        boolean last
) {}
