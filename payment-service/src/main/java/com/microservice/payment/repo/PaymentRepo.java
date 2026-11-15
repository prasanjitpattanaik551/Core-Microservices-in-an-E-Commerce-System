package com.microservice.payment.repo;

import com.microservice.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepo extends JpaRepository<PaymentOrder, Long> {
    PaymentOrder findByOrderId(String orderId);
    PaymentOrder findByEcommerceOrderId(Long ecommerceOrderId);
}
