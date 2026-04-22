package com.nyle.nylepay.services;

import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.models.User;
import com.nyle.nylepay.repositories.TransactionRepository;
import com.nyle.nylepay.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceMpesaTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private MpesaService mpesaService;

    @Mock
    private EmailService emailService;

    @Mock
    private BankTransferService bankTransferService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("test@nylepay.com");
    }

    @Test
    void processMpesaCallback_completesAndCreditsDeposit() {
        Transaction transaction = new Transaction();
        transaction.setId(11L);
        transaction.setUserId(1L);
        transaction.setCurrency("KSH");
        transaction.setAmount(new BigDecimal("1000"));
        transaction.setStatus("PENDING");
        transaction.setExternalId("ws_CO_123");
        transaction.setTimestamp(LocalDateTime.now());

        when(mpesaService.extractTransactionDetails(anyMap())).thenReturn(Map.of(
                "CheckoutRequestID", "ws_CO_123",
                "ResultCode", "0",
                "Amount", "1000",
                "MpesaReceiptNumber", "NLJ7RT61SV",
                "PhoneNumber", "254712345678"
        ));
        when(transactionRepository.findByExternalId("ws_CO_123")).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        transactionService.processMpesaCallback(Map.of());

        verify(walletService).addBalance(1L, "KSH", new BigDecimal("1000"));
        verify(userRepository).save(argThat(savedUser ->
                "254712345678".equals(savedUser.getMpesaNumber())));
        verify(transactionRepository).save(argThat(savedTransaction ->
                "COMPLETED".equals(savedTransaction.getStatus())
                        && "ws_CO_123".equals(savedTransaction.getExternalId())
                        && savedTransaction.getMetadata().contains("NLJ7RT61SV")));
        verify(emailService).sendTransactionNotification(user, transaction);
    }

    @Test
    void processMpesaCallback_ignoresDuplicateCompletion() {
        Transaction transaction = new Transaction();
        transaction.setId(12L);
        transaction.setUserId(1L);
        transaction.setCurrency("KSH");
        transaction.setAmount(new BigDecimal("500"));
        transaction.setStatus("COMPLETED");
        transaction.setExternalId("ws_CO_456");

        when(mpesaService.extractTransactionDetails(anyMap())).thenReturn(Map.of(
                "CheckoutRequestID", "ws_CO_456",
                "ResultCode", "0"
        ));
        when(transactionRepository.findByExternalId("ws_CO_456")).thenReturn(Optional.of(transaction));

        transactionService.processMpesaCallback(Map.of());

        verify(walletService, never()).addBalance(1L, "KSH", new BigDecimal("500"));
        verify(transactionRepository, never()).save(transaction);
        verify(emailService, never()).sendTransactionNotification(user, transaction);
    }

    @Test
    void processMpesaDisbursementResult_completesWithdrawalAndCreatesFeeRecord() {
        Transaction transaction = new Transaction();
        transaction.setId(13L);
        transaction.setUserId(1L);
        transaction.setType("WITHDRAW");
        transaction.setProvider("MPESA");
        transaction.setCurrency("KSH");
        transaction.setAmount(new BigDecimal("2000"));
        transaction.setStatus("PROCESSING");
        transaction.setExternalId("AG_20260419_123456");
        transaction.setMetadata("{\"fee\":27}");

        when(mpesaService.extractDisbursementDetails(anyMap())).thenReturn(Map.of(
                "ConversationID", "AG_20260419_123456",
                "ResultCode", "0"
        ));
        when(transactionRepository.findByExternalId("AG_20260419_123456")).thenReturn(Optional.of(transaction));
        when(transactionRepository.findByExternalId("FEE_AG_20260419_123456")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        transactionService.processMpesaDisbursementResult(Map.of());

        ArgumentCaptor<Transaction> savedTransactions = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(savedTransactions.capture());

        List<Transaction> captured = savedTransactions.getAllValues();
        assertTrue(captured.stream().anyMatch(saved ->
                "FEE".equals(saved.getType())
                        && new BigDecimal("-27").compareTo(saved.getAmount()) == 0));
        assertTrue(captured.stream().anyMatch(saved ->
                saved.getId() != null && "COMPLETED".equals(saved.getStatus())));
        verify(walletService, never()).addBalance(1L, "KSH", new BigDecimal("2027"));
        verify(emailService).sendTransactionNotification(user, transaction);
    }

    @Test
    void createBankDepositIntent_createsPendingBankDepositWithReferenceMetadata() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction transaction = transactionService.createBankDepositIntent(
                1L,
                new BigDecimal("2500"),
                "KES",
                "KCB",
                "KCB Bank",
                "00123456789",
                "Jane Doe",
                "KE",
                null);

        assertEquals("BANK", transaction.getProvider());
        assertEquals("DEPOSIT", transaction.getType());
        assertEquals("PENDING", transaction.getStatus());
        assertEquals("KSH", transaction.getCurrency());
        assertTrue(transaction.getExternalId().startsWith("BNK_DEP_1_"));
        assertTrue(transaction.getMetadata().contains("\"flow\":\"BANK_TO_WALLET\""));
        assertTrue(transaction.getMetadata().contains("\"sourceBankCode\":\"KCB\""));
    }

    @Test
    void processBankCallback_completesDepositAndCreditsWalletOnce() {
        Transaction transaction = new Transaction();
        transaction.setId(21L);
        transaction.setUserId(1L);
        transaction.setType("DEPOSIT");
        transaction.setProvider("BANK");
        transaction.setCurrency("KSH");
        transaction.setAmount(new BigDecimal("1500"));
        transaction.setStatus("PENDING");
        transaction.setExternalId("BNK_DEP_1_123");
        transaction.setMetadata("{\"flow\":\"BANK_TO_WALLET\"}");

        when(transactionRepository.findByExternalId("BNK_DEP_1_123")).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        transactionService.processBankCallback(Map.of(
                "status", "successful",
                "reference", "BNK_DEP_1_123"
        ));

        verify(walletService).addBalance(1L, "KSH", new BigDecimal("1500"));
        verify(transactionRepository).save(argThat(saved ->
                "COMPLETED".equals(saved.getStatus())
                        && saved.getMetadata().contains("lastBankCallback")));
        verify(emailService).sendTransactionNotification(user, transaction);
    }

    @Test
    void processBankCallback_completesWithdrawalWithoutRecreditingWallet() {
        Transaction transaction = new Transaction();
        transaction.setId(22L);
        transaction.setUserId(1L);
        transaction.setType("WITHDRAW");
        transaction.setProvider("BANK");
        transaction.setCurrency("KSH");
        transaction.setAmount(new BigDecimal("3000"));
        transaction.setStatus("PROCESSING");
        transaction.setExternalId("BNK_WDR_ABC");
        transaction.setMetadata("{\"fee\":500,\"providerTransferId\":\"999001\"}");

        when(transactionRepository.findByExternalId("BNK_WDR_ABC")).thenReturn(Optional.of(transaction));
        when(transactionRepository.findByExternalId("FEE_BNK_WDR_ABC")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        transactionService.processBankCallback(Map.of(
                "status", "completed",
                "reference", "BNK_WDR_ABC",
                "id", "999001"
        ));

        ArgumentCaptor<Transaction> savedTransactions = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(savedTransactions.capture());
        assertTrue(savedTransactions.getAllValues().stream().anyMatch(saved ->
                "COMPLETED".equals(saved.getStatus()) && "WITHDRAW".equals(saved.getType())));
        assertTrue(savedTransactions.getAllValues().stream().anyMatch(saved ->
                "FEE".equals(saved.getType()) && new BigDecimal("-500").compareTo(saved.getAmount()) == 0));
        verify(walletService, never()).addBalance(1L, "KSH", new BigDecimal("3000"));
        verify(emailService).sendTransactionNotification(user, transaction);
    }

    @Test
    void processBankCallback_failedWithdrawalRefundsWallet() {
        Transaction transaction = new Transaction();
        transaction.setId(23L);
        transaction.setUserId(1L);
        transaction.setType("WITHDRAW");
        transaction.setProvider("BANK");
        transaction.setCurrency("KSH");
        transaction.setAmount(new BigDecimal("1000"));
        transaction.setStatus("PROCESSING");
        transaction.setExternalId("BNK_WDR_FAIL");
        transaction.setMetadata("{\"fee\":100}");

        when(transactionRepository.findByExternalId("BNK_WDR_FAIL")).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        transactionService.processBankCallback(Map.of(
                "status", "failed",
                "reference", "BNK_WDR_FAIL"
        ));

        verify(walletService).addBalance(1L, "KSH", new BigDecimal("1100"));
        verify(transactionRepository).save(argThat(saved ->
                "FAILED".equals(saved.getStatus())
                        && saved.getMetadata().contains("\"refundAmount\":1100")));
        verify(emailService).sendTransactionNotification(user, transaction);
    }
}
