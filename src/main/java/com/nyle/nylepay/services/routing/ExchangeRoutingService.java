package com.nyle.nylepay.services.routing;

import com.nyle.nylepay.services.BankTransferService;
import com.nyle.nylepay.services.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExchangeRoutingService {

    private final BankTransferService bankTransferService;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExchangeRoutingService(BankTransferService bankTransferService,
                                  TransactionService transactionService) {
        this.bankTransferService = bankTransferService;
        this.transactionService = transactionService;
    }

    /**
     * Move funds from a Bank account to M-Pesa.
     */
    public Map<String, Object> moveBankToMpesa(Long userId, String bankCode, String accountNumber, BigDecimal amount, String mpesaNumber) throws Exception {
        
        // Step 1: Initiate Bank Pull (Deposit)
        // We define a routing metadata that tells the system to push to M-Pesa upon success
        Map<String, String> routingMeta = new HashMap<>();
        routingMeta.put("nextLeg", "MPESA_PUSH");
        routingMeta.put("mpesaNumber", mpesaNumber);
        
        String metadataJson = objectMapper.writeValueAsString(routingMeta);
        
        // Use KE for local banks
        Map<String, Object> bankResponse = bankTransferService.initiateLocalBankTransfer(
                "KE", accountNumber, bankCode, amount, "KSH", "NylePay Routing to M-Pesa"
        );
        
        // Create transaction with metadata
        transactionService.createDeposit(userId, "BANK", amount, "KSH", (String) bankResponse.get("id"), metadataJson);
        
        return Map.of(
            "message", "Bank to M-Pesa move initiated. Waiting for bank confirmation.",
            "bankResponse", bankResponse
        );
    }

    /**
     * Move funds from CEX to Bank.
     */
    public Map<String, Object> moveCexToBank(Long userId, String asset, BigDecimal amount, String bankCode, String accountNumber) throws Exception {
        // Step 1: Sell CEX asset to Fiat (KES)
        // Need to check if sellToFiat is implemented in CEX providers for KES
        
        // Step 2: Once fiat is available in NylePay wallet, push to Bank
        // For simplicity in this demo, we'll assume the CEX sell puts money in the NylePay wallet
        // and then we trigger a bank push.
        
        // This usually involves the CexRoutingService logic
        return Map.of("error", "Multi-stage CEX to Bank move requires further provider support for KES settle.");
    }
}
