package com.dev.order.repository;

import com.dev.order.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("""
    SELECT p 
    FROM Payment p 
    JOIN FETCH p.order 
    WHERE p.idempotencyKey = :key
    """)
    Optional<Payment> findByIdempotencyKeyWithOrder(@Param("key") String idempotencyKey);
}
