package com.nyle.nylepay.repositories;

import com.nyle.nylepay.models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Find transactions by user ID with pagination
    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    // Find transactions by user ID and status
    List<Transaction> findByUserIdAndStatus(Long userId, String status);

    // Find transaction by external ID
    Optional<Transaction> findByExternalId(String externalId);

    // Find transactions by provider and status
    List<Transaction> findByProviderAndStatus(String provider, String status);

    // Find transactions by status
    List<Transaction> findByStatus(String status);

    // Find pending transactions before a certain timestamp
    List<Transaction> findByStatusAndTimestampBefore(String status, LocalDateTime timestamp);

    // Find transactions within a date range
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.timestamp BETWEEN :startDate AND :endDate")
    List<Transaction> findByUserIdAndTimestampBetween(@Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Get total deposit amount for a user
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = 'DEPOSIT' AND t.status = 'COMPLETED'")
    BigDecimal getTotalDepositsByUser(@Param("userId") Long userId);

    // Get total withdrawal amount for a user
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = 'WITHDRAW' AND t.status = 'COMPLETED'")
    BigDecimal getTotalWithdrawalsByUser(@Param("userId") Long userId);

    // Get recent transactions for dashboard
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY t.timestamp DESC")
    List<Transaction> findRecentTransactions(@Param("userId") Long userId);

    long countByUserId(Long userId);

    List<Transaction> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
