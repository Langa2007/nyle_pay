package com.nyle.nylepay.services;

import com.nyle.nylepay.services.providers.FlutterwaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Routes outgoing bank transfers to the correct Flutterwave endpoint.
 *
 * Supported countries:
 *   KE — Kenya (Equity, KCB, Co-op, NCBA, Absa, DTB, I&M, Standard Chartered, Family Bank, …)
 *   NG, GH, TZ, UG, RW — other African corridors via Flutterwave's generic transfer API
 *
 * Security:
 *   - No bank account numbers are logged.
 *   - All outbound calls authenticated via Bearer ${flutterwave.secret-key}.
 *   - Live-mode guard: set flutterwave.live-mode=true in production.
 */
@Service
public class BankTransferService {

    private static final Logger log = LoggerFactory.getLogger(BankTransferService.class);

    @Value("${flutterwave.live-mode:false}")
    private boolean liveMode;

    private final FlutterwaveService flutterwaveService;

    public BankTransferService(FlutterwaveService flutterwaveService) {
        this.flutterwaveService = flutterwaveService;
    }

    /**
     * Initiates a local bank transfer.
     *
     * @param country       ISO-3166-1 alpha-2 country code (KE, NG, GH, …)
     * @param accountNumber beneficiary bank account number
     * @param bankCode      Flutterwave bank code (e.g. "KCB" or "044" for Kenya)
     * @param amount        transfer amount in the given currency
     * @param currency      ISO-4217 currency code (KES, NGN, GHS, …)
     * @param narration     payment description shown to beneficiary
     */
    public Map<String, Object> initiateLocalBankTransfer(
            String country,
            String accountNumber,
            String bankCode,
            BigDecimal amount,
            String currency,
            String narration) {

        if (!liveMode) {
            log.warn("[SANDBOX] Simulating bank transfer: {} {} {}/{} → {}",
                     amount, currency, country, bankCode, maskAccount(accountNumber));
            return Map.of(
                "status",    "SIMULATED",
                "id",        "BNK_SIM_" + System.currentTimeMillis(),
                "reference", "BNK_SIM_REF_" + System.currentTimeMillis(),
                "message",   "Bank transfer simulated. Set flutterwave.live-mode=true for production."
            );
        }

        log.info("Initiating bank transfer: {} {} to bank {}/{} in {}",
                 amount, currency, bankCode, maskAccount(accountNumber), country);
        // All country corridors use the same Flutterwave Transfers API — no special-casing needed
        return flutterwaveService.initiateTransfer(accountNumber, bankCode, amount, currency, narration);
    }

    /**
     * Verifies account name before linking or transferring (Flutterwave account resolution).
     */
    public Map<String, Object> resolveAccount(String accountNumber, String bankCode) {
        if (!liveMode) {
            log.warn("[SANDBOX] Simulating account resolution for bank {}", bankCode);
            return Map.of("status", "SIMULATED", "account_name", "SANDBOX ACCOUNT NAME");
        }
        return flutterwaveService.resolveAccount(accountNumber, bankCode);
    }

    // Mask account for logging — show only last 4 digits
    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) return "****";
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
