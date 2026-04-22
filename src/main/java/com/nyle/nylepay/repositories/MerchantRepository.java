package com.nyle.nylepay.repositories;

import com.nyle.nylepay.models.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByUserId(Long userId);
    Optional<Merchant> findByPublicKey(String publicKey);
    List<Merchant> findByStatus(String status);
}
