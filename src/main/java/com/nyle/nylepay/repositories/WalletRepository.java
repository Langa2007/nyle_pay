
package com.nyle.nylepay.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nyle.nylepay.models.Wallet;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    /**
     * Acquires a database-level exclusive row lock (SELECT ... FOR UPDATE).
     * Must be called within an active @Transactional context.
     * Use this for ALL balance mutations to prevent lost-update races.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("SELECT w FROM Wallet w WHERE w.userId = ?1 AND KEY(w.balances) = ?2")
    Optional<Wallet> findByUserIdAndBalances_CurrencyCode(Long userId, String currencyCode);
}
