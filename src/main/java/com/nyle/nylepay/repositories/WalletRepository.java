
// WalletRepository.java
package com.nyle.nylepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nyle.nylepay.models.Wallet;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    @Query("SELECT w FROM Wallet w WHERE w.userId = ?1 AND KEY(w.balances) = ?2")
    Optional<Wallet> findByUserIdAndBalances_CurrencyCode(Long userId, String currencyCode);
}