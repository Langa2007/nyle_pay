
package com.nyle.nylepay.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.repositories.TransactionRepository;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyle.nylepay.models.CheckoutSession;
import com.nyle.nylepay.repositories.CheckoutSessionRepository;
import com.nyle.nylepay.services.merchant.SettlementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyle.nylepay.exceptions.NylePayException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Transactional
    public Transaction createCryptoDeposit(Long userId, String asset, BigDecimal amount, String txHash) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("txHash", txHash);
        metadata.put("asset", asset);
        metadata.put("flow", "CRYPTO_TO_WALLET");

        Transaction transaction = createDeposit(
                userId,
                "CRYPTO",
                amount,
                asset,
                txHash,
                writeMetadata(metadata));
        transaction.setTransactionCode(generateTransactionCode("DEPOSIT", "CRYPTO"));
        transaction.setStatus("COMPLETED"); // Auto-credited
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createCryptoWithdrawal(Long userId, String asset, BigDecimal amount, String destination,
            String txHash) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType("WITHDRAW");
        transaction.setProvider("CRYPTO");
        transaction.setAmount(amount);
        transaction.setCurrency(asset);
        transaction.setStatus("COMPLETED");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setExternalId(txHash);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("destination", destination);
        metadata.put("txHash", txHash);
        transaction.setMetadata(writeMetadata(metadata));
        transaction.setTransactionCode(generateTransactionCode("WITHDRAW", "CRYPTO"));

        return transactionRepository.save(transaction);
    }

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final MpesaService mpesaService;
    private final EmailService emailService;
    private final BankTransferService bankTransferService;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final SettlementService settlementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionService(TransactionRepository transactionRepository,
            UserRepository userRepository,
            WalletService walletService,
            MpesaService mpesaService,
            EmailService emailService,
            BankTransferService bankTransferService,
            CheckoutSessionRepository checkoutSessionRepository,
            SettlementService settlementService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.mpesaService = mpesaService;
        this.emailService = emailService;
        this.bankTransferService = bankTransferService;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.settlementService = settlementService;
    }

    @Transactional
    public Transaction createDeposit(Long userId, String provider, BigDecimal amount,
            String currency, String externalId, String metadata) {

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType("DEPOSIT");
        transaction.setProvider(provider);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus("PENDING");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setExternalId(externalId);
        transaction.setMetadata(metadata);
        transaction.setTransactionCode(generateTransactionCode("DEPOSIT", provider));

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createWithdrawal(Long userId, String provider, BigDecimal amount,
            String currency, String destination) {

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }

        BigDecimal currentBalance = walletService.getBalance(userId, currency);
        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        BigDecimal minimumWithdrawal = getMinimumWithdrawal(currency, provider);
        if (amount.compareTo(minimumWithdrawal) < 0) {
            throw new RuntimeException("Minimum withdrawal amount is " + minimumWithdrawal + " " + currency);
        }

        BigDecimal fee = calculateWithdrawalFee(amount, currency, provider);
        BigDecimal totalDeduction = amount.add(fee);

        if (currentBalance.compareTo(totalDeduction) < 0) {
            throw new RuntimeException("Insufficient balance to cover withdrawal fee");
        }

        walletService.subtractBalance(userId, currency, totalDeduction);

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType("WITHDRAW");
        transaction.setProvider(provider);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus("PROCESSING");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setExternalId("WDR_" + System.currentTimeMillis() + "_" + userId);
        transaction.setTransactionCode(generateTransactionCode("WITHDRAW", provider));

        if ("MPESA".equalsIgnoreCase(provider)) {
            try {
                Map<String, Object> mpesaResult = mpesaService.initiateB2C(
                        destination, amount, "NylePay Withdrawal");
                String conversationId = firstNonBlank(
                        mpesaResult.get("ConversationID"),
                        mpesaResult.get("OriginatorConversationID"),
                        transaction.getExternalId());

                transaction.setExternalId(conversationId);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("destination", destination);
                metadata.put("fee", fee);
                metadata.put("requestedAmount", amount);
                metadata.put("totalDeduction", totalDeduction);
                metadata.put("originatorConversationId", mpesaResult.get("OriginatorConversationID"));
                metadata.put("conversationId", mpesaResult.get("ConversationID"));
                metadata.put("providerResponse", mpesaResult);
                transaction.setMetadata(writeMetadata(metadata));

            } catch (Exception e) {
                transaction.setStatus("FAILED");
                transaction.setExternalId("FAILED_" + transaction.getExternalId());
                walletService.addBalance(userId, currency, totalDeduction);
                throw new RuntimeException("Withdrawal failed: " + e.getMessage(), e);
            }

        } else if ("BANK".equalsIgnoreCase(provider)) {
            // destination format: "accountNumber|bankCode|country|accountName"
            String[] parts = destination.split("\\|");
            String accountNumber = parts.length > 0 ? parts[0] : destination;
            String bankCode = parts.length > 1 ? parts[1] : "";
            String country = parts.length > 2 ? parts[2] : "KE";
            String accountName = parts.length > 3 ? parts[3] : "";
            try {
                Map<String, Object> bankResult = bankTransferService.initiateLocalBankTransfer(
                        country, accountNumber, bankCode, amount, currency,
                        "NylePay Withdrawal " + transaction.getExternalId());
                String transferReference = extractBankReference(bankResult, transaction.getExternalId());
                String providerTransferId = extractBankTransferId(bankResult);
                transaction.setExternalId(transferReference);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("destination", destination);
                metadata.put("accountNumber", accountNumber);
                metadata.put("bankCode", bankCode);
                metadata.put("country", country);
                metadata.put("accountName", accountName);
                metadata.put("fee", fee);
                metadata.put("requestedAmount", amount);
                metadata.put("totalDeduction", totalDeduction);
                if (providerTransferId != null) {
                    metadata.put("providerTransferId", providerTransferId);
                }
                metadata.put("providerReference", transferReference);
                metadata.put("providerResponse", bankResult);
                transaction.setMetadata(writeMetadata(metadata));
            } catch (Exception e) {
                transaction.setStatus("FAILED");
                walletService.addBalance(userId, currency, totalDeduction);
                throw new RuntimeException("Bank withdrawal failed: " + e.getMessage(), e);
            }
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createBankDepositIntent(Long userId, BigDecimal amount, String currency,
            String bankCode, String bankName, String accountNumber, String accountName,
            String country, String requestedReference) {

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }
        if (bankCode == null || bankCode.isBlank()) {
            throw new RuntimeException("Bank code is required for bank deposits");
        }
        String sanitizedAccountNumber = sanitizeBankAccountNumber(accountNumber);
        if (sanitizedAccountNumber == null || sanitizedAccountNumber.isBlank()) {
            throw new RuntimeException("Bank account number is required for bank deposits");
        }

        String normalizedCountry = country == null || country.isBlank()
                ? "KE"
                : country.trim().toUpperCase();
        String normalizedCurrency = normalizeBankCurrency(currency);
        String reference = requestedReference != null && !requestedReference.isBlank()
                ? requestedReference.trim().toUpperCase()
                : generateBankReference(userId, "DEP");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sourceBankCode", bankCode.trim().toUpperCase());
        metadata.put("sourceBankName", bankName != null ? bankName.trim() : "");
        metadata.put("sourceAccountNumber", sanitizedAccountNumber);
        metadata.put("sourceAccountName", accountName != null ? accountName.trim() : "");
        metadata.put("country", normalizedCountry);
        metadata.put("flow", "BANK_TO_WALLET");
        metadata.put("collectionReference", reference);

        return createDeposit(
                userId,
                "BANK",
                amount,
                normalizedCurrency,
                reference,
                writeMetadata(metadata));
    }

    @Transactional
    public Transaction createTransfer(Long fromUserId, String toIdentifier,
            BigDecimal amount, String currency, String description) {

        User recipient = findRecipient(toIdentifier);
        if (recipient == null) {
            throw new RuntimeException("Recipient not found");
        }

        BigDecimal senderBalance = walletService.getBalance(fromUserId, currency);
        if (senderBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        BigDecimal fee = calculateTransferFee(amount, currency);
        BigDecimal totalDeduction = amount.add(fee);

        if (senderBalance.compareTo(totalDeduction) < 0) {
            throw new RuntimeException("Insufficient balance to cover transfer fee");
        }

        walletService.subtractBalance(fromUserId, currency, totalDeduction);
        walletService.addBalance(recipient.getId(), currency, amount);
        String transferReference = "TRF_" + System.currentTimeMillis() + "_" + fromUserId + "_" + recipient.getId();

        Transaction senderTransaction = new Transaction();
        senderTransaction.setUserId(fromUserId);
        senderTransaction.setType("TRANSFER_OUT");
        senderTransaction.setProvider("NYLEPAY");
        senderTransaction.setAmount(amount.negate());
        senderTransaction.setCurrency(currency);
        senderTransaction.setStatus("COMPLETED");
        senderTransaction.setTimestamp(LocalDateTime.now());
        senderTransaction.setExternalId(transferReference + "_OUT");
        senderTransaction.setTransactionCode(generateTransactionCode("TRANSFER_OUT", "NYLEPAY"));
        Map<String, Object> senderMetadata = new HashMap<>();
        senderMetadata.put("transferReference", transferReference);
        senderMetadata.put("recipientUserId", recipient.getId());
        senderMetadata.put("recipientIdentifier", toIdentifier);
        senderMetadata.put("description", description);
        senderMetadata.put("reversalStatus", "NONE");
        senderTransaction.setMetadata(writeMetadata(senderMetadata));
        Transaction savedSenderTransaction = transactionRepository.save(senderTransaction);

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTransaction = new Transaction();
            feeTransaction.setUserId(fromUserId);
            feeTransaction.setType("FEE");
            feeTransaction.setProvider("NYLEPAY");
            feeTransaction.setAmount(fee.negate());
            feeTransaction.setCurrency(currency);
            feeTransaction.setStatus("COMPLETED");
            feeTransaction.setTimestamp(LocalDateTime.now());
            feeTransaction.setExternalId("FEE_" + savedSenderTransaction.getExternalId());
            transactionRepository.save(feeTransaction);
        }

        Transaction recipientTransaction = new Transaction();
        recipientTransaction.setUserId(recipient.getId());
        recipientTransaction.setType("TRANSFER_IN");
        recipientTransaction.setProvider("NYLEPAY");
        recipientTransaction.setAmount(amount);
        recipientTransaction.setCurrency(currency);
        recipientTransaction.setStatus("COMPLETED");
        recipientTransaction.setTimestamp(LocalDateTime.now());
        recipientTransaction.setExternalId(transferReference + "_IN");
        recipientTransaction.setTransactionCode(generateTransactionCode("TRANSFER_IN", "NYLEPAY"));
        Map<String, Object> recipientMetadata = new HashMap<>();
        recipientMetadata.put("transferReference", transferReference);
        recipientMetadata.put("senderUserId", fromUserId);
        recipientMetadata.put("senderTransactionId", savedSenderTransaction.getId());
        recipientMetadata.put("description", description);
        recipientTransaction.setMetadata(writeMetadata(recipientMetadata));

        Transaction savedRecipientTransaction = transactionRepository.save(recipientTransaction);
        mergeMetadata(savedSenderTransaction, Map.of("recipientTransactionId", savedRecipientTransaction.getId()));
        transactionRepository.save(savedSenderTransaction);

        return savedRecipientTransaction;
    }

    @Transactional
    public Map<String, Object> requestTransferReversal(Long senderUserId, Long transactionId,
            String reason, String contactPhone) {
        Transaction senderTransaction = resolveSenderTransfer(transactionId);
        if (!senderTransaction.getUserId().equals(senderUserId)) {
            throw new RuntimeException("Only the sender can request reversal for this transaction");
        }
        if (!"COMPLETED".equalsIgnoreCase(senderTransaction.getStatus())) {
            throw new RuntimeException("Only completed transfers can be reviewed for reversal");
        }

        Map<String, Object> metadata = readMetadata(senderTransaction);
        if (metadata.get("recipientUserId") == null) {
            throw new RuntimeException("This transfer is missing recipient linkage and cannot be reversed automatically");
        }
        String currentStatus = asString(metadata.get("reversalStatus"));
        if (currentStatus != null && !"NONE".equalsIgnoreCase(currentStatus)) {
            throw new RuntimeException("A reversal workflow is already recorded for this transfer: " + currentStatus);
        }

        metadata.put("reversalStatus", "REQUESTED");
        metadata.put("reversalReason", reason);
        metadata.put("senderContactPhone", contactPhone);
        metadata.put("reversalRequestedAt", LocalDateTime.now().toString());
        senderTransaction.setMetadata(writeMetadata(metadata));
        transactionRepository.save(senderTransaction);

        return Map.of(
                "transactionId", senderTransaction.getId(),
                "status", "REQUESTED",
                "message", "Reversal request received. NylePay support will call the recipient before actioning the case.");
    }

    @Transactional
    public Map<String, Object> resolveTransferReversal(Long transactionId, String recipientOutcome,
            Long supportAgentUserId, String notes) {
        Transaction senderTransaction = resolveSenderTransfer(transactionId);
        Map<String, Object> metadata = readMetadata(senderTransaction);
        Long recipientUserId = asLong(metadata.get("recipientUserId"));
        if (recipientUserId == null) {
            throw new RuntimeException("This transfer is missing recipient linkage");
        }

        String outcome = recipientOutcome == null ? "" : recipientOutcome.trim().toUpperCase();
        metadata.put("supportAgentUserId", supportAgentUserId);
        metadata.put("supportNotes", notes);
        metadata.put("recipientCallOutcome", outcome);
        metadata.put("reversalReviewedAt", LocalDateTime.now().toString());

        if (outcome.equals("EXPECTED_FUNDS") || outcome.equals("RECIPIENT_DISPUTES")) {
            metadata.put("reversalStatus", "DISPUTED_POLICE_REPORT_REQUIRED");
            senderTransaction.setMetadata(writeMetadata(metadata));
            transactionRepository.save(senderTransaction);
            return Map.of(
                    "transactionId", senderTransaction.getId(),
                    "status", "DISPUTED_POLICE_REPORT_REQUIRED",
                    "message", "Recipient says they expected the funds. Advise sender to report the matter to police for further reversal action.");
        }

        boolean canReverse = outcome.equals("NO_RESPONSE")
                || outcome.equals("PHONE_OFF")
                || outcome.equals("RECIPIENT_UNREACHABLE")
                || outcome.equals("RECIPIENT_CONSENTS");
        if (!canReverse) {
            throw new RuntimeException("Unsupported recipient outcome: " + recipientOutcome);
        }

        BigDecimal amount = senderTransaction.getAmount().abs();
        BigDecimal recipientBalance = walletService.getBalance(recipientUserId, senderTransaction.getCurrency());
        if (recipientBalance.compareTo(amount) < 0) {
            metadata.put("reversalStatus", "INSUFFICIENT_RECIPIENT_FUNDS");
            metadata.put("recipientBalanceAtReview", recipientBalance);
            senderTransaction.setMetadata(writeMetadata(metadata));
            transactionRepository.save(senderTransaction);
            return Map.of(
                    "transactionId", senderTransaction.getId(),
                    "status", "INSUFFICIENT_RECIPIENT_FUNDS",
                    "message", "Recipient account does not have enough available funds to complete the reversal.");
        }

        walletService.subtractBalanceForSystem(recipientUserId, senderTransaction.getCurrency(), amount);
        walletService.addBalance(senderTransaction.getUserId(), senderTransaction.getCurrency(), amount);

        String reversalReference = "REV_" + System.currentTimeMillis() + "_" + senderTransaction.getId();
        Transaction senderReversal = createReversalLedger(
                senderTransaction.getUserId(), "REVERSAL_IN", amount, senderTransaction.getCurrency(),
                reversalReference + "_IN", senderTransaction, outcome, notes);
        Transaction recipientReversal = createReversalLedger(
                recipientUserId, "REVERSAL_OUT", amount.negate(), senderTransaction.getCurrency(),
                reversalReference + "_OUT", senderTransaction, outcome, notes);

        metadata.put("reversalStatus", "COMPLETED");
        metadata.put("reversedAt", LocalDateTime.now().toString());
        metadata.put("senderReversalTransactionId", senderReversal.getId());
        metadata.put("recipientReversalTransactionId", recipientReversal.getId());
        senderTransaction.setStatus("REVERSED");
        senderTransaction.setMetadata(writeMetadata(metadata));
        transactionRepository.save(senderTransaction);

        Long recipientTransactionId = asLong(metadata.get("recipientTransactionId"));
        if (recipientTransactionId != null) {
            transactionRepository.findById(recipientTransactionId).ifPresent(tx -> {
                tx.setStatus("REVERSED");
                mergeMetadata(tx, Map.of("reversalStatus", "COMPLETED",
                        "senderReversalTransactionId", senderReversal.getId(),
                        "recipientReversalTransactionId", recipientReversal.getId()));
                transactionRepository.save(tx);
            });
        }

        return Map.of(
                "transactionId", senderTransaction.getId(),
                "status", "COMPLETED",
                "amount", amount,
                "currency", senderTransaction.getCurrency(),
                "message", "Reversal completed from recipient available balance.");
    }

    @Transactional
    public Transaction createConversion(Long userId, String fromCurrency,
            String toCurrency, BigDecimal amount) {

        BigDecimal sourceBalance = walletService.getBalance(userId, fromCurrency);
        if (sourceBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in " + fromCurrency);
        }

        BigDecimal exchangeRate = getExchangeRate(fromCurrency, toCurrency);
        BigDecimal convertedAmount = amount.multiply(exchangeRate);

        BigDecimal fee = convertedAmount.multiply(BigDecimal.valueOf(0.01)); // 1% fee
        BigDecimal finalAmount = convertedAmount.subtract(fee);

        walletService.subtractBalance(userId, fromCurrency, amount);
        walletService.addBalance(userId, toCurrency, finalAmount);

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType("CONVERSION");
        transaction.setProvider("EXCHANGE");
        transaction.setAmount(amount);
        transaction.setCurrency(fromCurrency);
        transaction.setStatus("COMPLETED");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setExternalId("CVT_" + System.currentTimeMillis());
        transaction.setTransactionCode(generateTransactionCode("CONVERSION", "EXCHANGE"));

        Transaction savedTransaction = transactionRepository.save(transaction);

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
        Map<String, Object> details = mpesaService.extractTransactionDetails(callbackData);
        String checkoutRequestId = asString(details.get("CheckoutRequestID"));
        String resultCode = asString(details.get("ResultCode"));
        String mpesaReceiptNumber = asString(details.get("MpesaReceiptNumber"));
        BigDecimal callbackAmount = asBigDecimal(details.get("Amount"));
        String phoneNumber = asString(details.get("PhoneNumber"));

        if (checkoutRequestId == null) {
            logger.warn("Ignoring MPesa callback without CheckoutRequestID: {}", callbackData);
            return;
        }

        Optional<Transaction> pendingTransaction = transactionRepository
                .findByExternalId(checkoutRequestId);

        if (pendingTransaction.isEmpty()) {
            // Check if this is a merchant CheckoutSession
            Optional<CheckoutSession> sessionOpt = checkoutSessionRepository.findByProviderIntentId(checkoutRequestId);
            if (sessionOpt.isPresent()) {
                CheckoutSession session = sessionOpt.get();
                if ("COMPLETED".equals(session.getStatus())) {
                    logger.info("Ignoring duplicate MPesa callback for settled CheckoutSession {}", session.getReference());
                    return;
                }
                
                if ("0".equals(resultCode)) {
                    session.setStatus("COMPLETED");
                    checkoutSessionRepository.save(session);
                    settlementService.settleMerchantRealTime(session, session.getAmount());
                    logger.info("CheckoutSession {} completed via MPesa real-time settlement", session.getReference());
                } else {
                    session.setStatus("FAILED");
                    checkoutSessionRepository.save(session);
                }
                return;
            }

            logger.warn("No MPesa transaction or CheckoutSession found for CheckoutRequestID={}", checkoutRequestId);
            return;
        }

        Transaction transaction = pendingTransaction.get();
        if (isFinalStatus(transaction.getStatus())) {
            // Safaricom can retry webhooks after the transaction is already final.
            logger.info("Ignoring duplicate MPesa callback for settled transaction {}", transaction.getId());
            return;
        }

        Map<String, Object> callbackMetadata = new HashMap<>();
        callbackMetadata.put("lastMpesaCallback", details);
        callbackMetadata.put("mpesaReceiptNumber", mpesaReceiptNumber);
        callbackMetadata.put("mpesaPhoneNumber", phoneNumber);
        mergeMetadata(transaction, callbackMetadata);

        if ("0".equals(resultCode)) {
            BigDecimal amountToCredit = callbackAmount != null ? callbackAmount : transaction.getAmount();
            if (callbackAmount != null && callbackAmount.compareTo(transaction.getAmount()) != 0) {
                mergeMetadata(transaction, Map.of(
                        "reviewRequired", true,
                        "amountMismatch", true,
                        "requestedAmount", transaction.getAmount(),
                        "callbackAmount", callbackAmount));
                logger.warn("MPesa callback amount mismatch for transaction {}. requested={}, callback={}",
                        transaction.getId(), transaction.getAmount(), callbackAmount);
            }

            transaction.setStatus("COMPLETED");
            walletService.addBalance(
                    transaction.getUserId(),
                    transaction.getCurrency(),
                    amountToCredit);

            updateUserMpesaNumberIfMissing(transaction.getUserId(), phoneNumber);
        } else {
            transaction.setStatus("FAILED");
        }

        try {
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            // A concurrent duplicate callback can pass the first lookup before the winning
            // transaction commits; let the transaction roll back if the unique key catches it.
            logger.warn(
                    "Duplicate MPesa callback rejected by unique constraint for CheckoutRequestID={} — already processed.",
                    checkoutRequestId);
            throw e;
        }
        // Send notifications after persistence so email failures do not roll back settlement.
        notifyUser(transaction);
    }

    @Transactional
    public void processMpesaDisbursementResult(Map<String, Object> callbackData) {
        Map<String, Object> details = mpesaService.extractDisbursementDetails(callbackData);
        String reference = asString(firstNonBlank(
                details.get("ConversationID"),
                details.get("OriginatorConversationID")));

        if (reference == null) {
            logger.warn("Ignoring MPesa disbursement result without conversation reference: {}", callbackData);
            return;
        }

        Transaction transaction = findMpesaWithdrawal(reference)
                .orElseThrow(() -> new RuntimeException("MPesa withdrawal transaction not found for " + reference));

        if (isFinalStatus(transaction.getStatus())) {
            // Safaricom may retry callbacks for already-settled withdrawals.
            logger.info("Ignoring duplicate MPesa disbursement result for transaction {}", transaction.getId());
            return;
        }

        mergeMetadata(transaction, Map.of("lastMpesaDisbursementResult", details));
        if ("0".equals(asString(details.get("ResultCode")))) {
            transaction.setStatus("COMPLETED");
            createWithdrawalFeeTransaction(transaction);
        } else {
            transaction.setStatus("FAILED");
            refundWithdrawal(transaction);
        }

        // Keep status, refund, fee, and notification changes in the same transaction boundary.
        transactionRepository.save(transaction);
        notifyUser(transaction);
    }

    @Transactional
    public void processMpesaDisbursementTimeout(Map<String, Object> callbackData) {
        Map<String, Object> details = mpesaService.extractDisbursementDetails(callbackData);
        String reference = asString(firstNonBlank(
                details.get("ConversationID"),
                details.get("OriginatorConversationID")));

        if (reference == null) {
            logger.warn("Ignoring MPesa timeout callback without conversation reference: {}", callbackData);
            return;
        }

        Transaction transaction = findMpesaWithdrawal(reference)
                .orElseThrow(() -> new RuntimeException("MPesa withdrawal transaction not found for " + reference));

        if (isFinalStatus(transaction.getStatus())) {
            // Timeout callbacks can arrive after a final result callback.
            logger.info("Ignoring MPesa timeout for settled transaction {}", transaction.getId());
            return;
        }

        mergeMetadata(transaction, Map.of("lastMpesaTimeout", details));
        transaction.setStatus("FAILED");
        refundWithdrawal(transaction);

        // Keep status, refund, fee, and notification changes in the same transaction boundary.
        transactionRepository.save(transaction);
        notifyUser(transaction);
    }

    @Transactional
    public void processBankCallback(Map<String, Object> callbackData) {
        String reference = extractBankReference(callbackData, null);
        String providerTransferId = extractBankTransferId(callbackData);

        if (reference == null && providerTransferId == null) {
            logger.warn("Ignoring bank callback without reference or provider transfer id: {}", callbackData);
            return;
        }

        String status = extractBankStatus(callbackData);
        Optional<Transaction> transactionOpt = findBankTransaction(reference, providerTransferId);
        if (transactionOpt.isEmpty()) {
            logger.warn("No bank transaction found for reference={} providerTransferId={}", reference,
                    providerTransferId);
            return;
        }

        Transaction transaction = transactionOpt.get();

        if (isFinalStatus(transaction.getStatus())) {
            logger.info("Ignoring duplicate bank callback for settled transaction {}", transaction.getId());
            return;
        }

        mergeMetadata(transaction, Map.of(
                "lastBankCallback", callbackData,
                "providerReference", reference != null ? reference : "",
                "providerTransferId", providerTransferId != null ? providerTransferId : ""));

        if (isSuccessfulBankStatus(status)) {
            handleSuccessfulBankCallback(transaction);
        } else if (isFailedBankStatus(status)) {
            handleFailedBankCallback(transaction);
        } else {
            logger.info("Bank callback received non-final status '{}' for transaction {}", status, transaction.getId());
            transactionRepository.save(transaction);
            return;
        }

        try {
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate bank callback rejected by unique constraint for reference={}", reference);
            throw e;
        }
        notifyUser(transaction);
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

        long totalTransactions = transactionRepository.countByUserId(userId);
        long successfulCount = transactionRepository.findByUserIdAndStatus(userId, "COMPLETED").size();
        long failedCount = transactionRepository.findByUserIdAndStatus(userId, "FAILED").size();
        long pendingCount = transactionRepository.findByUserIdAndStatus(userId, "PENDING").size();

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

            if ("PENDING".equals(oldStatus) && "COMPLETED".equals(status) &&
                    "DEPOSIT".equals(transaction.getType())) {
                walletService.addBalance(
                        transaction.getUserId(),
                        transaction.getCurrency(),
                        transaction.getAmount());
            }

            transactionRepository.save(transaction);

            User user = userRepository.findById(transaction.getUserId()).orElse(null);
            if (user != null) {
                emailService.sendTransactionNotification(user, transaction);
            }
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

            User user = userRepository.findById(transaction.getUserId()).orElse(null);
            if (user != null) {
                emailService.sendTransactionNotification(user, transaction);
            }
        }
    }

    private Transaction resolveSenderTransfer(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if ("TRANSFER_OUT".equalsIgnoreCase(transaction.getType())) {
            return transaction;
        }
        if ("TRANSFER_IN".equalsIgnoreCase(transaction.getType())) {
            Long senderTransactionId = asLong(readMetadata(transaction).get("senderTransactionId"));
            if (senderTransactionId != null) {
                return transactionRepository.findById(senderTransactionId)
                        .orElseThrow(() -> new RuntimeException("Sender transaction not found"));
            }
        }
        throw new RuntimeException("Only NylePay wallet transfers can be reversed through this workflow");
    }

    private Transaction createReversalLedger(Long userId, String type, BigDecimal amount, String currency,
            String externalId, Transaction originalTransaction, String outcome, String notes) {
        Transaction reversal = new Transaction();
        reversal.setUserId(userId);
        reversal.setType(type);
        reversal.setProvider("NYLEPAY_SUPPORT");
        reversal.setAmount(amount);
        reversal.setCurrency(currency);
        reversal.setStatus("COMPLETED");
        reversal.setTimestamp(LocalDateTime.now());
        reversal.setExternalId(externalId);
        reversal.setTransactionCode(generateTransactionCode(type, "NYLEPAY"));
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("originalTransactionId", originalTransaction.getId());
        metadata.put("originalTransactionCode", originalTransaction.getTransactionCode());
        metadata.put("recipientCallOutcome", outcome);
        metadata.put("supportNotes", notes);
        reversal.setMetadata(writeMetadata(metadata));
        return transactionRepository.save(reversal);
    }

    private User findRecipient(String identifier) {
        Optional<User> byEmail = userRepository.findByEmail(identifier);
        if (byEmail.isPresent())
            return byEmail.get();

        Optional<User> byAccountNumber = userRepository.findByAccountNumber(identifier);
        if (byAccountNumber.isPresent())
            return byAccountNumber.get();

        if (identifier.matches("^2547[0-9]{8}$")) {
            Optional<User> byPhone = userRepository.findByMpesaNumber(identifier);
            if (byPhone.isPresent())
                return byPhone.get();
        }

        try {
            Long userId = Long.parseLong(identifier);
            Optional<User> byId = userRepository.findById(userId);
            if (byId.isPresent())
                return byId.get();
        } catch (NumberFormatException e) {
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
            fee = amount.multiply(BigDecimal.valueOf(0.01));
            BigDecimal minimumFee = "USD".equals(currency) ? BigDecimal.valueOf(5) : BigDecimal.valueOf(500);
            if (fee.compareTo(minimumFee) < 0) {
                fee = minimumFee;
            }
            BigDecimal maximumFee = "USD".equals(currency) ? BigDecimal.valueOf(50) : BigDecimal.valueOf(5000);
            if (fee.compareTo(maximumFee) > 0) {
                fee = maximumFee;
            }
        } else if ("CRYPTO".equalsIgnoreCase(provider)) {
            BigDecimal networkFee = "ETH".equals(currency) ? BigDecimal.valueOf(0.0005) : BigDecimal.valueOf(0.00005);
            BigDecimal serviceFee = amount.multiply(BigDecimal.valueOf(0.005));
            fee = networkFee.add(serviceFee);
        }

        return fee;
    }

    private BigDecimal calculateTransferFee(BigDecimal amount, String currency) {
        BigDecimal fee = amount.multiply(BigDecimal.valueOf(0.005));
        BigDecimal minimumFee = "USD".equals(currency) ? BigDecimal.valueOf(0.01) : BigDecimal.valueOf(1);

        if (fee.compareTo(minimumFee) < 0) {
            return minimumFee;
        }

        BigDecimal maximumFee = "USD".equals(currency) ? BigDecimal.valueOf(10) : BigDecimal.valueOf(1000);

        if (fee.compareTo(maximumFee) > 0) {
            return maximumFee;
        }

        return fee;
    }

    private BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
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

    public boolean hasSufficientBalance(Long userId, String currency, BigDecimal amount) {
        BigDecimal balance = walletService.getBalance(userId, currency);
        return balance.compareTo(amount) >= 0;
    }

    public BigDecimal calculateTotalFees(Long userId, String period) {
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

    private Optional<Transaction> findMpesaWithdrawal(String reference) {
        Optional<Transaction> direct = transactionRepository.findByExternalId(reference)
                .filter(transaction -> "WITHDRAW".equals(transaction.getType()));
        if (direct.isPresent()) {
            return direct;
        }

        List<Transaction> candidates = new ArrayList<>();
        candidates.addAll(transactionRepository.findByProviderAndStatus("MPESA", "PROCESSING"));
        candidates.addAll(transactionRepository.findByProviderAndStatus("MPESA", "FAILED"));
        candidates.addAll(transactionRepository.findByProviderAndStatus("MPESA", "COMPLETED"));

        return candidates.stream()
                .filter(transaction -> "WITHDRAW".equals(transaction.getType()))
                .filter(transaction -> reference.equals(transaction.getExternalId())
                        || (transaction.getMetadata() != null && transaction.getMetadata().contains(reference)))
                .findFirst();
    }

    private Optional<Transaction> findBankTransaction(String reference, String providerTransferId) {
        if (reference != null) {
            Optional<Transaction> direct = transactionRepository.findByExternalId(reference)
                    .filter(transaction -> "BANK".equalsIgnoreCase(transaction.getProvider()));
            if (direct.isPresent()) {
                return direct;
            }
        }

        List<Transaction> candidates = new ArrayList<>();
        candidates.addAll(transactionRepository.findByProviderAndStatus("BANK", "PENDING"));
        candidates.addAll(transactionRepository.findByProviderAndStatus("BANK", "PROCESSING"));
        candidates.addAll(transactionRepository.findByProviderAndStatus("BANK", "FAILED"));
        candidates.addAll(transactionRepository.findByProviderAndStatus("BANK", "COMPLETED"));

        return candidates.stream()
                .filter(transaction -> matchesBankCallback(transaction, reference, providerTransferId))
                .findFirst();
    }

    private boolean matchesBankCallback(Transaction transaction, String reference, String providerTransferId) {
        if (reference != null && reference.equals(transaction.getExternalId())) {
            return true;
        }
        Map<String, Object> metadata = readMetadata(transaction);
        if (reference != null) {
            String providerReference = asString(metadata.get("providerReference"));
            String collectionReference = asString(metadata.get("collectionReference"));
            if (reference.equals(providerReference) || reference.equals(collectionReference)) {
                return true;
            }
        }
        if (providerTransferId != null) {
            String savedTransferId = asString(metadata.get("providerTransferId"));
            return providerTransferId.equals(savedTransferId);
        }
        return false;
    }

    private void handleSuccessfulBankCallback(Transaction transaction) {
        if ("DEPOSIT".equalsIgnoreCase(transaction.getType())) {
            transaction.setStatus("COMPLETED");
            walletService.addBalance(transaction.getUserId(), transaction.getCurrency(), transaction.getAmount());
            dispatchBankRoutingLegs(transaction);
            return;
        }

        if ("WITHDRAW".equalsIgnoreCase(transaction.getType())) {
            transaction.setStatus("COMPLETED");
            createWithdrawalFeeTransaction(transaction);
            return;
        }

        transaction.setStatus("COMPLETED");
    }

    private void handleFailedBankCallback(Transaction transaction) {
        transaction.setStatus("FAILED");
        if ("WITHDRAW".equalsIgnoreCase(transaction.getType())) {
            refundWithdrawal(transaction);
        }
    }

    private void dispatchBankRoutingLegs(Transaction transaction) {
        if (transaction.getMetadata() == null) {
            return;
        }

        Map<String, Object> meta = readMetadata(transaction);
        if (!"MPESA_PUSH".equals(meta.get("nextLeg"))) {
            return;
        }

        String mpesaNumber = asString(meta.get("mpesaNumber"));
        if (mpesaNumber == null) {
            throw new NylePayException(
                    "Bank->M-Pesa routing metadata missing mpesaNumber for tx " + transaction.getId());
        }

        // If the next leg fails, the surrounding transaction rolls back the wallet credit.
        mpesaService.initiateB2C(mpesaNumber, transaction.getAmount(), "NylePay Bank->M-Pesa");
        logger.info("Bank->M-Pesa routing dispatched: tx={} mpesa={}", transaction.getId(), mpesaNumber);
    }

    private void refundWithdrawal(Transaction transaction) {
        Map<String, Object> metadata = readMetadata(transaction);
        if (Boolean.TRUE.equals(metadata.get("refunded"))) {
            return;
        }

        BigDecimal fee = asBigDecimal(metadata.get("fee"));
        if (fee == null) {
            fee = calculateWithdrawalFee(transaction.getAmount(), transaction.getCurrency(), transaction.getProvider());
        }
        BigDecimal refundAmount = transaction.getAmount().add(fee);
        walletService.addBalance(transaction.getUserId(), transaction.getCurrency(), refundAmount);

        metadata.put("refunded", true);
        metadata.put("refundAmount", refundAmount);
        transaction.setMetadata(writeMetadata(metadata));
    }

    private void createWithdrawalFeeTransaction(Transaction transaction) {
        Map<String, Object> metadata = readMetadata(transaction);
        BigDecimal fee = asBigDecimal(metadata.get("fee"));
        if (fee == null || fee.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String feeExternalId = "FEE_" + transaction.getExternalId();
        if (transactionRepository.findByExternalId(feeExternalId).isPresent()) {
            return;
        }

        Transaction feeTransaction = new Transaction();
        feeTransaction.setUserId(transaction.getUserId());
        feeTransaction.setType("FEE");
        feeTransaction.setProvider(transaction.getProvider());
        feeTransaction.setAmount(fee.negate());
        feeTransaction.setCurrency(transaction.getCurrency());
        feeTransaction.setStatus("COMPLETED");
        feeTransaction.setTimestamp(LocalDateTime.now());
        feeTransaction.setExternalId(feeExternalId);
        transactionRepository.save(feeTransaction);
    }

    private void notifyUser(Transaction transaction) {
        User user = userRepository.findById(transaction.getUserId()).orElse(null);
        if (user != null) {
            emailService.sendTransactionNotification(user, transaction);
        }
    }

    private void updateUserMpesaNumberIfMissing(Long userId, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null && (user.getMpesaNumber() == null || user.getMpesaNumber().isBlank())) {
            user.setMpesaNumber(phoneNumber);
            userRepository.save(user);
        }
    }

    private boolean isFinalStatus(String status) {
        return "COMPLETED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status);
    }

    private boolean isSuccessfulBankStatus(String status) {
        return "success".equalsIgnoreCase(status)
                || "successful".equalsIgnoreCase(status)
                || "completed".equalsIgnoreCase(status);
    }

    private boolean isFailedBankStatus(String status) {
        return "failed".equalsIgnoreCase(status)
                || "error".equalsIgnoreCase(status)
                || "cancelled".equalsIgnoreCase(status)
                || "reversed".equalsIgnoreCase(status);
    }

    private Map<String, Object> readMetadata(Transaction transaction) {
        if (transaction.getMetadata() == null || transaction.getMetadata().isBlank()) {
            return new HashMap<>();
        }

        try {
            Map<?, ?> raw = objectMapper.readValue(transaction.getMetadata(), Map.class);
            Map<String, Object> metadata = new HashMap<>();
            raw.forEach((key, value) -> metadata.put(String.valueOf(key), value));
            return metadata;
        } catch (Exception e) {
            logger.warn("Failed to parse metadata for transaction {}", transaction.getId(), e);
            return new HashMap<>();
        }
    }

    private void mergeMetadata(Transaction transaction, Map<String, Object> updates) {
        Map<String, Object> metadata = readMetadata(transaction);
        metadata.putAll(updates);
        transaction.setMetadata(writeMetadata(metadata));
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to write transaction metadata", e);
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String stringValue = String.valueOf(value).trim();
        return stringValue.isEmpty() || "null".equalsIgnoreCase(stringValue) ? null : stringValue;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String stringValue = asString(value);
            if (stringValue != null) {
                return stringValue;
            }
        }
        return null;
    }

    private String extractBankReference(Map<String, Object> payload, String fallback) {
        if (payload == null) {
            return fallback;
        }
        String directReference = firstNonBlank(
                payload.get("reference"),
                payload.get("tx_ref"),
                payload.get("flw_ref"));
        if (directReference != null) {
            return directReference;
        }
        Object dataValue = payload.get("data");
        if (dataValue instanceof Map<?, ?> rawMap) {
            Map<String, Object> data = new HashMap<>();
            rawMap.forEach((k, v) -> data.put(String.valueOf(k), v));
            return firstNonBlank(
                    data.get("reference"),
                    data.get("tx_ref"),
                    data.get("flw_ref"),
                    fallback);
        }
        return fallback;
    }

    private String extractBankTransferId(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        String directId = asString(payload.get("id"));
        if (directId != null) {
            return directId;
        }
        Object dataValue = payload.get("data");
        if (dataValue instanceof Map<?, ?> rawMap) {
            Map<String, Object> data = new HashMap<>();
            rawMap.forEach((k, v) -> data.put(String.valueOf(k), v));
            return asString(data.get("id"));
        }
        return null;
    }

    private String extractBankStatus(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        String directStatus = asString(payload.get("status"));
        if (directStatus != null) {
            return directStatus;
        }
        Object dataValue = payload.get("data");
        if (dataValue instanceof Map<?, ?> rawMap) {
            Map<String, Object> data = new HashMap<>();
            rawMap.forEach((k, v) -> data.put(String.valueOf(k), v));
            return asString(data.get("status"));
        }
        return null;
    }

    private String sanitizeBankAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            return null;
        }
        return accountNumber.replaceAll("[^A-Za-z0-9]", "");
    }

    private String normalizeBankCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "KSH";
        }
        String normalized = currency.trim().toUpperCase();
        return "KES".equals(normalized) ? "KSH" : normalized;
    }

    private String generateBankReference(Long userId, String flow) {
        return "BNK_" + flow + "_" + userId + "_" + System.currentTimeMillis();
    }


    /**
     * Creates a local payment transaction (B2B or B2C).
     *
     * @param userId        the paying user
     * @param paymentType   TILL, PAYBILL, POCHI, or SEND_MONEY
     * @param amount        amount in KES
     * @param destination   till number, paybill shortcode, or phone number
     * @param accountRef    account reference (for paybill/pochi) or null
     * @param mpesaResponse raw response from Safaricom API
     */
    @Transactional
    public Transaction createLocalPayment(Long userId, String paymentType,
            BigDecimal amount, String destination,
            String accountRef,
            Map<String, Object> mpesaResponse) {

        String externalId = null;
        if (mpesaResponse != null) {
            externalId = asString(mpesaResponse.get("ConversationID"));
            if (externalId == null) {
                externalId = asString(mpesaResponse.get("OriginatorConversationID"));
            }
        }
        if (externalId == null) {
            externalId = "LOCAL_" + paymentType + "_" + userId + "_" + System.currentTimeMillis();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentType", paymentType);
        metadata.put("destination", destination);
        if (accountRef != null)
            metadata.put("accountReference", accountRef);
        if (mpesaResponse != null)
            metadata.put("mpesaResponse", mpesaResponse);

        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType("LOCAL_PAYMENT");
        transaction.setProvider("MPESA_" + paymentType);
        transaction.setAmount(amount);
        transaction.setCurrency("KSH");
        transaction.setStatus("PENDING");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setExternalId(externalId);
        transaction.setMetadata(writeMetadata(metadata));
        transaction.setTransactionCode(generateTransactionCode("LOCAL_PAYMENT", paymentType));

        Transaction saved = transactionRepository.save(transaction);
        logger.info("Local payment created: userId={} type={} amount={} destination={} txId={}",
                userId, paymentType, amount, destination, saved.getId());

        notifyUser(saved);

        return saved;
    }

    /**
     * Processes Safaricom B2B callback for Till/Paybill/Pochi payments.
     * Called by the B2B result webhook endpoint.
     */
    @Transactional
    public void processB2BCallback(Map<String, Object> payload) {
        logger.info("Processing B2B callback: {}", payload);

        String conversationId = asString(payload.get("ConversationID"));
        String originatorId = asString(payload.get("OriginatorConversationID"));
        Object resultCodeObj = payload.get("ResultCode");
        int resultCode = resultCodeObj != null ? Integer.parseInt(String.valueOf(resultCodeObj)) : -1;

        String lookupId = conversationId != null ? conversationId : originatorId;
        if (lookupId == null) {
            logger.warn("B2B callback missing ConversationID and OriginatorConversationID");
            return;
        }

        Optional<Transaction> txOpt = transactionRepository.findByExternalId(lookupId);
        if (txOpt.isEmpty() && originatorId != null) {
            txOpt = transactionRepository.findByExternalId(originatorId);
        }

        if (txOpt.isEmpty()) {
            logger.warn("B2B callback: no matching transaction for conversationId={}", lookupId);
            return;
        }

        Transaction transaction = txOpt.get();
        if (isFinalStatus(transaction.getStatus())) {
            logger.info("B2B callback: transaction {} already in final status {}", transaction.getId(),
                    transaction.getStatus());
            return;
        }

        if (resultCode == 0) {
            transaction.setStatus("COMPLETED");
            logger.info("B2B payment COMPLETED: txId={} type={}", transaction.getId(), transaction.getProvider());
        } else {
            transaction.setStatus("FAILED");
            walletService.addBalance(transaction.getUserId(), "KSH", transaction.getAmount());
            logger.warn("B2B payment FAILED: txId={} resultCode={}", transaction.getId(), resultCode);
        }

        mergeMetadata(transaction, Map.of("b2bResultCode", resultCode, "b2bCallback", payload));
        transactionRepository.save(transaction);
        notifyUser(transaction);
    }

    private String generateTransactionCode(String type, String provider) {
        String prefix = "NYL";
        String upperType = type != null ? type.toUpperCase() : "";
        String upperProv = provider != null ? provider.toUpperCase() : "";

        if (upperType.contains("DEPOSIT")) {
            if (upperProv.contains("MPESA"))
                prefix = "MP2NP";
            else if (upperProv.contains("BANK"))
                prefix = "BK2NP";
            else if (upperProv.contains("CRYPTO"))
                prefix = "CX2NP";
            else
                prefix = "DP2NP";
        } else if (upperType.contains("WITHDRAW")) {
            if (upperProv.contains("MPESA"))
                prefix = "NP2MP";
            else if (upperProv.contains("BANK"))
                prefix = "NP2BK";
            else if (upperProv.contains("CRYPTO"))
                prefix = "NP2CX";
            else
                prefix = "NP2WD";
        } else if (upperType.contains("TRANSFER")) {
            prefix = "NP2NP";
        } else if (upperType.contains("CONVERSION")) {
            prefix = "NPXCH";
        } else if (upperType.contains("LOCAL_PAYMENT") || upperType.contains("TILL") || upperType.contains("PAYBILL")
                || upperType.contains("POCHI")) {
            prefix = "NPPAY";
        }

        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + "-" + randomPart;
    }
}
