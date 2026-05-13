
package com.nyle.nylepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nyle.nylepay.models.PaymentMethod;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUserId(Long userId);
    Optional<PaymentMethod> findByUserIdAndType(Long userId, String type);
    List<PaymentMethod> findByUserIdAndIsDefaultTrue(Long userId);
    Optional<PaymentMethod> findByUserIdAndIsVerifiedTrueAndType(Long userId, String type);
}
