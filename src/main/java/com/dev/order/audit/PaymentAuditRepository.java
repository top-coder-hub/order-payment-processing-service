package com.dev.order.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface PaymentAuditRepository extends JpaRepository<PaymentAudit, Long> {
}
