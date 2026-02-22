/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 4:26 am
 */
package com.dev.order.repository;

import com.dev.order.domain.Order;
import com.dev.order.domain.OrderState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
    //For the 'All' view
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    //For the 'Filtered' view
    Page<Order> findByCustomerIdAndOrderState(
            Long customerId,
            OrderState orderState,
            Pageable pageable
    );
}
