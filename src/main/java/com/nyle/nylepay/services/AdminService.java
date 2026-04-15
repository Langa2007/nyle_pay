package com.nyle.nylepay.services;

import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.TransactionRepository;
import com.nyle.nylepay.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AdminService(UserRepository userRepository,
                        TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    // ────────────────────────────────────────────────────────────────
    // DASHBOARD METRICS
    // ────────────────────────────────────────────────────────────────
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // User counts
        long totalUsers = userRepository.count();
        metrics.put("totalUsers", totalUsers);

        // Transaction counts
        long totalTransactions = transactionRepository.count();
        List<Transaction> allTransactions = transactionRepository.findAll();

        long completed = allTransactions.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long pending = allTransactions.stream().filter(t -> "PENDING".equals(t.getStatus())).count();
        long failed = allTransactions.stream().filter(t -> "FAILED".equals(t.getStatus())).count();

        metrics.put("totalTransactions", totalTransactions);
        metrics.put("completedTransactions", completed);
        metrics.put("pendingTransactions", pending);
        metrics.put("failedTransactions", failed);

        // Success rate
        double successRate = totalTransactions > 0
                ? (double) completed / totalTransactions * 100
                : 0;
        metrics.put("successRate", BigDecimal.valueOf(successRate).setScale(1, RoundingMode.HALF_UP));

        // Volume by currency
        Map<String, BigDecimal> volumeByCurrency = new LinkedHashMap<>();
        for (Transaction t : allTransactions) {
            if ("COMPLETED".equals(t.getStatus()) && t.getAmount() != null) {
                volumeByCurrency.merge(
                    t.getCurrency() != null ? t.getCurrency() : "UNKNOWN",
                    t.getAmount(),
                    BigDecimal::add
                );
            }
        }
        metrics.put("volumeByCurrency", volumeByCurrency);

        // Transaction types breakdown
        Map<String, Long> typeBreakdown = new LinkedHashMap<>();
        for (Transaction t : allTransactions) {
            typeBreakdown.merge(t.getType() != null ? t.getType() : "UNKNOWN", 1L, Long::sum);
        }
        metrics.put("transactionTypes", typeBreakdown);

        // Last 7 days daily volume
        List<Map<String, Object>> dailyVolume = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            List<Transaction> dayTransactions = transactionRepository
                    .findByTimestampBetween(dayStart, dayEnd);

            BigDecimal dayTotal = dayTransactions.stream()
                    .filter(t -> "COMPLETED".equals(t.getStatus()) && t.getAmount() != null)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", dayStart.toLocalDate().toString());
            dayData.put("volume", dayTotal);
            dayData.put("count", dayTransactions.size());
            dailyVolume.add(dayData);
        }
        metrics.put("dailyVolume", dailyVolume);

        return metrics;
    }

    // ────────────────────────────────────────────────────────────────
    // TRANSACTION MANAGEMENT
    // ────────────────────────────────────────────────────────────────
    public Page<Transaction> getAllTransactions(int page, int size, String status) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        if (status != null && !status.isEmpty()) {
            return transactionRepository.findByStatus(status, pageRequest);
        }
        return transactionRepository.findAll(pageRequest);
    }

    // ────────────────────────────────────────────────────────────────
    // USER MANAGEMENT
    // ────────────────────────────────────────────────────────────────
    public Page<User> getAllUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
    }

    public Map<String, Object> getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Transaction> userTransactions = transactionRepository.findByUserId(userId);

        BigDecimal totalVolume = userTransactions.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus()) && t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("user", user);
        detail.put("totalTransactions", userTransactions.size());
        detail.put("totalVolume", totalVolume);
        detail.put("recentTransactions", userTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .toList());

        return detail;
    }
}
