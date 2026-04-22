package com.nyle.nylepay.repositories;

import com.nyle.nylepay.models.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByTransactionId(Long transactionId);
    List<Refund> findByCheckoutSessionId(Long checkoutSessionId);
    Optional<Refund> findByProviderRefundId(String providerRefundId);
    List<Refund> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);
}
