package com.nyle.nylepay.repositories;

import com.nyle.nylepay.models.CheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, Long> {
    Optional<CheckoutSession> findByReference(String reference);
    Optional<CheckoutSession> findByProviderIntentId(String providerIntentId);
    List<CheckoutSession> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);
    List<CheckoutSession> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<CheckoutSession> findByMerchantIdAndStatus(Long merchantId, String status);
}
