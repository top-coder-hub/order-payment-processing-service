/**
 * Created By Lavanyaa Karthik
 * Date: 05/03/26
 * Time: 2:16 am
 */
package com.dev.order.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface OrderAuditRepository extends JpaRepository<OrderAudit, Long> {
}
