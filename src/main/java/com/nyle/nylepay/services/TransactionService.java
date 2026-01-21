// TransactionService.java - UPDATED
package com.nyle.nylepay.services;

import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.repositories.TransactionRepository;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final MpesaService mpesaService;

    public TransactionService(TransactionRepository transactionRepository,
            UserRepository userRepository,
            WalletService walletService,
            MpesaService mpesaService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.mpesaService = mpesaService;
    }

    @Transactional
    public Transaction createDeposit(Long userId, String provider, BigDecimal amount,
            String currency, String externalId) {

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType("DEPOSIT");
        transaction.setProvider(provider);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus("PENDING");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setExternalId(externalId);

        // Save transaction
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createWithdrawal(Long userId, String provider, BigDecimal amount,
            String currency, String destination) {

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user has sufficient balance
        BigDecimal currentBalance = walletService.getBalance(userId, currency);
        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Validate minimum withdrawal amount
        BigDecimal minimumWithdrawal = getMinimumWithdrawal(currency, provider);
        if (amount.compareTo(minimumWithdrawal) < 0) {
            throw new RuntimeException("Minimum withdrawal amount is " + minimumWithdrawal + " " + currency);
        }

        // Calculate fees
        BigDecimal fee = calculateWithdrawalFee(amount, currency, provider);
        BigDecimal totalDeduction = amount.add(fee);

        // Check if user has enough for amount + fee
        if (currentBalance.compareTo(totalDeduction) < 0) {
            throw new RuntimeException("Insufficient balance to cover withdrawal fee");
        }

        // Deduct balance immediately
        walletService.subtractBalance(userId, currency, totalDeduction);

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType("WITHDRAW");
        transaction.setProvider(provider);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus("PROCESSING");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setExternalId("WDR_" + System.currentTimeMillis() + "_" + userId);

        // For MPesa withdrawals, initiate immediately
        if ("MPESA".equalsIgnoreCase(provider)) {
            try {
                // Initiate MPesa B2C payment
                Map<String, Object> mpesaResult = mpesaService.initiateB2C(
                        destination, amount, "NylePay Withdrawal");
                transaction.setExternalId((String) mpesaResult.get("ConversationID"));
                transaction.setStatus("COMPLETED");

                // Create a fee transaction
                if (fee.compareTo(BigDecimal.ZERO) > 0) {
                    Transaction feeTransaction = new Transaction();
                    feeTransaction.setUserId(userId);
                    feeTransaction.setType("FEE");
                    feeTransaction.setProvider(provider);
                    feeTransaction.setAmount(fee.negate());
                    feeTransaction.setCurrency(currency);
                    feeTransaction.setStatus("COMPLETED");
                    feeTransaction.setTimestamp(LocalDateTime.now());
                    feeTransaction.setExternalId("FEE_" + transaction.getExternalId());
                    transactionRepository.save(feeTransaction);
                }

            } catch (Exception e) {
                transaction.setStatus("FAILED");
                transaction.setExternalId("FAILED_" + transaction.getExternalId());
                // Refund the deducted amount
                walletService.addBalance(userId, currency, totalDeduction);
                throw new RuntimeException("Withdrawal failed: " + e.getMessage(), e);
            }
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createTransfer(Long fromUserId, String toIdentifier,
            BigDecimal amount, String currency, String description) {

        // Find recipient
        User recipient = findRecipient(toIdentifier);
        if (recipient == null) {
            throw new RuntimeException("Recipient not found");
        }

        // Check if sender has sufficient balance
        BigDecimal senderBalance = walletService.getBalance(fromUserId, currency);
        if (senderBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Calculate transfer fee
        BigDecimal fee = calculateTransferFee(amount, currency);
        BigDecimal totalDeduction = amount.add(fee);

        // Check if sender has enough for amount + fee
        if (senderBalance.compareTo(totalDeduction) < 0) {
            throw new RuntimeException("Insufficient balance to cover transfer fee");
        }

        // Perform transfer
        walletService.subtractBalance(fromUserId, currency, totalDeduction);
        walletService.addBalance(recipient.getId(), currency, amount);

        // Create transaction for sender
        Transaction senderTransaction = new Transaction();
        senderTransaction.setUserId(fromUserId);
        senderTransaction.setType("TRANSFER_OUT");
        senderTransaction.setProvider("NYLEPAY");
        senderTransaction.setAmount(amount.negate());
        senderTransaction.setCurrency(currency);
        senderTransaction.setStatus("COMPLETED");
        senderTransaction.setTimestamp(LocalDateTime.now());
        senderTransaction.setExternalId("TRF_" + System.currentTimeMillis());
        transactionRepository.save(senderTransaction);

        // Create fee transaction if applicable
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTransaction = new Transaction();
            feeTransaction.setUserId(fromUserId);
            feeTransaction.setType("FEE");
            feeTransaction.setProvider("NYLEPAY");
            feeTransaction.setAmount(fee.negate());
            feeTransaction.setCurrency(currency);
            feeTransaction.setStatus("COMPLETED");
            feeTransaction.setTimestamp(LocalDateTime.now());
            feeTransaction.setExternalId("FEE_" + senderTransaction.getExternalId());
            transactionRepository.save(feeTransaction);
        }

        // Create transaction for recipient
        Transaction recipientTransaction = new Transaction();
        recipientTransaction.setUserId(recipient.getId());
        recipientTransaction.setType("TRANSFER_IN");
        recipientTransaction.setProvider("NYLEPAY");
        recipientTransaction.setAmount(amount);
        recipientTransaction.setCurrency(currency);
        recipientTransaction.setStatus("COMPLETED");
        recipientTransaction.setTimestamp(LocalDateTime.now());
        recipientTransaction.setExternalId("TRF_" + System.currentTimeMillis());

        return transactionRepository.save(recipientTransaction);
    }

    @Transactional
    public Transaction createConversion(Long userId, String fromCurrency,
            String toCurrency, BigDecimal amount) {

        // Check if user has sufficient balance in source currency
        BigDecimal sourceBalance = walletService.getBalance(userId, fromCurrency);
        if (sourceBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in " + fromCurrency);
        }

        // Get exchange rate (in real implementation, fetch from exchange service)
        BigDecimal exchangeRate = getExchangeRate(fromCurrency, toCurrency);
        BigDecimal convertedAmount = amount.multiply(exchangeRate);

        // Calculate conversion fee
        BigDecimal fee = convertedAmount.multiply(BigDecimal.valueOf(0.01)); // 1% fee
        BigDecimal finalAmount = convertedAmount.subtract(fee);

        // Perform conversion
        walletService.subtractBalance(userId, fromCurrency, amount);
        walletService.addBalance(userId, toCurrency, finalAmount);

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType("CONVERSION");
        transaction.setProvider("EXCHANGE");
        transaction.setAmount(amount);
        transaction.setCurrency(fromCurrency);
        transaction.setStatus("COMPLETED");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setExternalId("CVT_" + System.currentTimeMillis());

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Create fee transaction
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTransaction = new Transaction();
            feeTransaction.setUserId(userId);
            feeTransaction.setType("FEE");
            feeTransaction.setProvider("EXCHANGE");
            feeTransaction.setAmount(fee.negate());
            feeTransaction.setCurrency(toCurrency);
            feeTransaction.setStatus("COMPLETED");
            feeTransaction.setTimestamp(LocalDateTime.now());
            feeTransaction.setExternalId("FEE_" + savedTransaction.getExternalId());
            transactionRepository.save(feeTransaction);
        }

        return savedTransaction;
    }

    @Transactional
    public void processMpesaCallback(Map<String, Object> callbackData) {
        try {
            String checkoutRequestId = (String) callbackData.get("CheckoutRequestID");
            String resultCode = (String) callbackData.get("ResultCode");
            String mpesaReceiptNumber = (String) callbackData.get("MpesaReceiptNumber");
            BigDecimal amount = new BigDecimal(callbackData.get("Amount").toString());
            String phoneNumber = (String) callbackData.get("PhoneNumber");

            // Find pending transaction
            Optional<Transaction> pendingTransaction = transactionRepository
                    .findByExternalId(checkoutRequestId);

            if (pendingTransaction.isPresent()) {
                Transaction transaction = pendingTransaction.get();

                if ("0".equals(resultCode)) {
                    // Success
                    transaction.setStatus("COMPLETED");
                    transaction.setExternalId(mpesaReceiptNumber);

                    // Update wallet balance
                    walletService.addBalance(
                            transaction.getUserId(),
                            transaction.getCurrency(),
                            amount);

                    // Update user's MPesa number if not set
                    User user = userRepository.findById(transaction.getUserId()).orElse(null);
                    if (user != null && user.getMpesaNumber() == null) {
                        user.setMpesaNumber(phoneNumber);
                        userRepository.save(user);
                    }
                } else {
                    // Failed
                    transaction.setStatus("FAILED");
                }

                transactionRepository.save(transaction);
            }

        } catch (Exception e) {
            // Log the error but don't throw to prevent MPesa from retrying
            e.printStackTrace();
        }
    }

    @Transactional
    public void processBankCallback(Map<String, Object> callbackData) {
        try {
            String reference = (String) callbackData.get("reference");
            String status = (String) callbackData.get("status");
            BigDecimal amount = new BigDecimal(callbackData.get("amount").toString());
            String currency = (String) callbackData.get("currency");

            // Find transaction by reference
            Optional<Transaction> transactionOpt = transactionRepository
                    .findByExternalId(reference);

            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();

                if ("success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                    transaction.setStatus("COMPLETED");
                    walletService.addBalance(
                            transaction.getUserId(),
                            currency,
                            amount);
                } else {
                    transaction.setStatus("FAILED");
                }

                transactionRepository.save(transaction);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> getUserTransactions(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<Transaction> transactionPage = transactionRepository.findByUserId(userId, pageable);
        return transactionPage.getContent();
    }

    public Map<String, Object> getTransactionStats(Long userId) {
        List<Transaction> allTransactions = transactionRepository.findByUserIdAndStatus(userId, "COMPLETED");

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalTransfersIn = BigDecimal.ZERO;
        BigDecimal totalTransfersOut = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;

        for (Transaction t : allTransactions) {
            switch (t.getType()) {
                case "DEPOSIT":
                    totalDeposits = totalDeposits.add(t.getAmount());
                    break;
                case "WITHDRAW":
                    totalWithdrawals = totalWithdrawals.add(t.getAmount());
                    break;
                case "TRANSFER_IN":
                    totalTransfersIn = totalTransfersIn.add(t.getAmount());
                    break;
                case "TRANSFER_OUT":
                    totalTransfersOut = totalTransfersOut.add(t.getAmount().abs());
                    break;
                case "FEE":
                    totalFees = totalFees.add(t.getAmount().abs());
                    break;
            }
        }

        // Get counts
        long totalTransactions = transactionRepository.countByUserId(userId);
        long successfulCount = transactionRepository.findByUserIdAndStatus(userId, "COMPLETED").size();
        long failedCount = transactionRepository.findByUserIdAndStatus(userId, "FAILED").size();
        long pendingCount = transactionRepository.findByUserIdAndStatus(userId, "PENDING").size();

        // Get first and last transaction dates
        List<Transaction> allUserTransactions = transactionRepository.findByUserId(userId,
                PageRequest.of(0, 1, Sort.by("timestamp").ascending())).getContent();
        List<Transaction> recentTransactions = transactionRepository.findByUserId(userId,
                PageRequest.of(0, 1, Sort.by("timestamp").descending())).getContent();

        LocalDateTime firstTransactionDate = allUserTransactions.isEmpty() ? null
                : allUserTransactions.get(0).getTimestamp();
        LocalDateTime lastTransactionDate = recentTransactions.isEmpty() ? null
                : recentTransactions.get(0).getTimestamp();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTransactions", totalTransactions);
        stats.put("successfulTransactions", successfulCount);
        stats.put("failedTransactions", failedCount);
        stats.put("pendingTransactions", pendingCount);
        stats.put("totalDeposits", totalDeposits);
        stats.put("totalWithdrawals", totalWithdrawals);
        stats.put("totalTransfersIn", totalTransfersIn);
        stats.put("totalTransfersOut", totalTransfersOut);
        stats.put("totalFees", totalFees);
        stats.put("netFlow", totalDeposits.subtract(totalWithdrawals).subtract(totalTransfersOut).subtract(totalFees));
        stats.put("firstTransaction", firstTransactionDate);
        stats.put("lastTransaction", lastTransactionDate);

        return stats;
    }

    @Transactional
    public void updateTransactionStatus(Long transactionId, String status, String notes) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);

        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            String oldStatus = transaction.getStatus();
            transaction.setStatus(status);

            // If status changed from PENDING to COMPLETED for deposit, update balance
            if ("PENDING".equals(oldStatus) && "COMPLETED".equals(status) &&
                    "DEPOSIT".equals(transaction.getType())) {
                walletService.addBalance(
                        transaction.getUserId(),
                        transaction.getCurrency(),
                        transaction.getAmount());
            }

            transactionRepository.save(transaction);

            // TODO: Send notification to user about status change
            // You would implement this with a notification service
        }
    }

    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findByStatus("PENDING");
    }

    public List<Transaction> getFailedTransactions() {
        return transactionRepository.findByStatus("FAILED");
    }

    public List<Transaction> getTransactionsByStatus(String status) {
        return transactionRepository.findByStatus(status);
    }

    public List<Transaction> getRecentTransactions(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("timestamp").descending());
        return transactionRepository.findByUserId(userId, pageable).getContent();
    }

    public Map<String, Object> getDailyTransactionSummary(LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);

        List<Transaction> dailyTransactions = transactionRepository
                .findByTimestampBetween(startOfDay, endOfDay);

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        int depositCount = 0;
        int withdrawalCount = 0;

        for (Transaction t : dailyTransactions) {
            if ("DEPOSIT".equals(t.getType()) && "COMPLETED".equals(t.getStatus())) {
                totalDeposits = totalDeposits.add(t.getAmount());
                depositCount++;
            } else if ("WITHDRAW".equals(t.getType()) && "COMPLETED".equals(t.getStatus())) {
                totalWithdrawals = totalWithdrawals.add(t.getAmount());
                withdrawalCount++;
            }
        }

        return Map.of(
                "date", date.toLocalDate(),
                "totalTransactions", dailyTransactions.size(),
                "depositCount", depositCount,
                "withdrawalCount", withdrawalCount,
                "totalDeposits", totalDeposits,
                "totalWithdrawals", totalWithdrawals,
                "netFlow", totalDeposits.subtract(totalWithdrawals));
    }

    @Transactional
    public void processTimeoutTransactions() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(24);
        List<Transaction> timedOutTransactions = transactionRepository
                .findByStatusAndTimestampBefore("PENDING", timeoutThreshold);

        for (Transaction transaction : timedOutTransactions) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);

            // TODO: Send notification to user about failed transaction
            // You would implement this with a notification service
        }
    }

    // Helper methods
    private User findRecipient(String identifier) {
        // Try by email
        Optional<User> byEmail = userRepository.findByEmail(identifier);
        if (byEmail.isPresent())
            return byEmail.get();

        // Try by phone (assuming identifier could be phone)
        if (identifier.matches("^2547[0-9]{8}$")) {
            Optional<User> byPhone = userRepository.findByMpesaNumber(identifier);
            if (byPhone.isPresent())
                return byPhone.get();
        }

        // Try by user ID
        try {
            Long userId = Long.parseLong(identifier);
            Optional<User> byId = userRepository.findById(userId);
            if (byId.isPresent())
                return byId.get();
        } catch (NumberFormatException e) {
            // Not a valid user ID
        }

        return null;
    }

    private BigDecimal getMinimumWithdrawal(String currency, String provider) {
        if ("MPESA".equalsIgnoreCase(provider) && "KSH".equals(currency)) {
            return BigDecimal.valueOf(100); // 100 KSH minimum for MPesa
        } else if ("BANK".equalsIgnoreCase(provider)) {
            return "USD".equals(currency) ? BigDecimal.valueOf(10) : BigDecimal.valueOf(1000);
        } else if ("CRYPTO".equalsIgnoreCase(provider)) {
            if ("ETH".equals(currency)) {
                return BigDecimal.valueOf(0.001); // 0.001 ETH minimum
            } else if ("BTC".equals(currency)) {
                return BigDecimal.valueOf(0.0001); // 0.0001 BTC minimum
            }
        }
        return BigDecimal.valueOf(10); // Default minimum
    }

    private BigDecimal calculateWithdrawalFee(BigDecimal amount, String currency, String provider) {
        BigDecimal fee = BigDecimal.ZERO;

        if ("MPESA".equalsIgnoreCase(provider)) {
            // MPesa charges based on amount ranges
            if (amount.compareTo(BigDecimal.valueOf(1000)) <= 0) {
                fee = BigDecimal.valueOf(27);
            } else if (amount.compareTo(BigDecimal.valueOf(2500)) <= 0) {
                fee = BigDecimal.valueOf(33);
            } else if (amount.compareTo(BigDecimal.valueOf(5000)) <= 0) {
                fee = BigDecimal.valueOf(55);
            } else if (amount.compareTo(BigDecimal.valueOf(15000)) <= 0) {
                fee = BigDecimal.valueOf(60);
            } else if (amount.compareTo(BigDecimal.valueOf(25000)) <= 0) {
                fee = BigDecimal.valueOf(75);
            } else if (amount.compareTo(BigDecimal.valueOf(35000)) <= 0) {
                fee = BigDecimal.valueOf(85);
            } else if (amount.compareTo(BigDecimal.valueOf(50000)) <= 0) {
                fee = BigDecimal.valueOf(95);
            } else if (amount.compareTo(BigDecimal.valueOf(150000)) <= 0) {
                fee = BigDecimal.valueOf(100);
            } else {
                fee = BigDecimal.valueOf(195);
            }
        } else if ("BANK".equalsIgnoreCase(provider)) {
            // Bank transfer fee: 1% of amount with minimum
            fee = amount.multiply(BigDecimal.valueOf(0.01));
            BigDecimal minimumFee = "USD".equals(currency) ? BigDecimal.valueOf(5) : BigDecimal.valueOf(500);
            if (fee.compareTo(minimumFee) < 0) {
                fee = minimumFee;
            }
            // Maximum fee cap
            BigDecimal maximumFee = "USD".equals(currency) ? BigDecimal.valueOf(50) : BigDecimal.valueOf(5000);
            if (fee.compareTo(maximumFee) > 0) {
                fee = maximumFee;
            }
        } else if ("CRYPTO".equalsIgnoreCase(provider)) {
            // Crypto withdrawal: Network fee + 0.5% service fee
            BigDecimal networkFee = "ETH".equals(currency) ? BigDecimal.valueOf(0.0005) : BigDecimal.valueOf(0.00005); // ETH
                                                                                                                       // vs
                                                                                                                       // BTC
            BigDecimal serviceFee = amount.multiply(BigDecimal.valueOf(0.005));
            fee = networkFee.add(serviceFee);
        }

        return fee;
    }

    private BigDecimal calculateTransferFee(BigDecimal amount, String currency) {
        // Internal transfer: 0.5% with minimum
        BigDecimal fee = amount.multiply(BigDecimal.valueOf(0.005));
        BigDecimal minimumFee = "USD".equals(currency) ? BigDecimal.valueOf(0.01) : BigDecimal.valueOf(1);

        if (fee.compareTo(minimumFee) < 0) {
            return minimumFee;
        }

        // Maximum fee cap
        BigDecimal maximumFee = "USD".equals(currency) ? BigDecimal.valueOf(10) : BigDecimal.valueOf(1000);

        if (fee.compareTo(maximumFee) > 0) {
            return maximumFee;
        }

        return fee;
    }

    private BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        // Hardcoded rates for demo - in production, fetch from exchange service
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD_KSH", BigDecimal.valueOf(150.0));
        rates.put("KSH_USD", BigDecimal.valueOf(0.0067));
        rates.put("USD_ETH", BigDecimal.valueOf(0.0003));
        rates.put("ETH_USD", BigDecimal.valueOf(3000.0));
        rates.put("KSH_ETH", BigDecimal.valueOf(0.00002));
        rates.put("ETH_KSH", BigDecimal.valueOf(450000.0));
        rates.put("USD_BTC", BigDecimal.valueOf(0.000022));
        rates.put("BTC_USD", BigDecimal.valueOf(45000.0));
        rates.put("ETH_BTC", BigDecimal.valueOf(0.067));
        rates.put("BTC_ETH", BigDecimal.valueOf(15.0));

        String key = fromCurrency + "_" + toCurrency;
        return rates.getOrDefault(key, BigDecimal.ONE);
    }

    // Additional utility methods
    public boolean hasSufficientBalance(Long userId, String currency, BigDecimal amount) {
        BigDecimal balance = walletService.getBalance(userId, currency);
        return balance.compareTo(amount) >= 0;
    }

    public BigDecimal calculateTotalFees(Long userId, String period) {
        // Calculate fees for a period (day, week, month)
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();

        switch (period.toUpperCase()) {
            case "DAY":
                startDate = endDate.minusDays(1);
                break;
            case "WEEK":
                startDate = endDate.minusWeeks(1);
                break;
            case "MONTH":
                startDate = endDate.minusMonths(1);
                break;
            default:
                startDate = endDate.minusDays(30); // Default to 30 days
        }

        List<Transaction> fees = transactionRepository.findByUserIdAndTimestampBetween(
                userId, startDate, endDate).stream()
                .filter(t -> "FEE".equals(t.getType()) && "COMPLETED".equals(t.getStatus()))
                .collect(Collectors.toList());

        BigDecimal totalFees = BigDecimal.ZERO;
        for (Transaction fee : fees) {
            totalFees = totalFees.add(fee.getAmount().abs());
        }

        return totalFees;
    }

    public Map<String, Map<String, BigDecimal>> getTransactionSummaryByCurrency(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndStatus(userId, "COMPLETED");

        Map<String, BigDecimal> depositsByCurrency = new HashMap<>();
        Map<String, BigDecimal> withdrawalsByCurrency = new HashMap<>();
        Map<String, BigDecimal> transfersByCurrency = new HashMap<>();

        for (Transaction t : transactions) {
            String currency = t.getCurrency();

            switch (t.getType()) {
                case "DEPOSIT":
                    depositsByCurrency.merge(currency, t.getAmount(), BigDecimal::add);
                    break;
                case "WITHDRAW":
                    withdrawalsByCurrency.merge(currency, t.getAmount(), BigDecimal::add);
                    break;
                case "TRANSFER_IN":
                case "TRANSFER_OUT":
                    transfersByCurrency.merge(currency, t.getAmount().abs(), BigDecimal::add);
                    break;
            }
        }

        Map<String, Map<String, BigDecimal>> summary = new HashMap<>();
        summary.put("deposits", depositsByCurrency);
        summary.put("withdrawals", withdrawalsByCurrency);
        summary.put("transfers", transfersByCurrency);

        return summary;
    }
}