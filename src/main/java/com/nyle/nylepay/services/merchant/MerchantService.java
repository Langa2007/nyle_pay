package com.nyle.nylepay.services.merchant;

import com.nyle.nylepay.models.CheckoutSession;
import com.nyle.nylepay.models.Merchant;
import com.nyle.nylepay.repositories.CheckoutSessionRepository;
import com.nyle.nylepay.repositories.MerchantRepository;
import com.nyle.nylepay.utils.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages merchant accounts and payment links.
 *
 * Security:
 *   - Secret keys are AES-256-GCM encrypted at rest (same as CEX keys).
 *   - Public keys are safe to expose in frontend code.
 *   - Webhook secrets are sent to merchants once at registration — not fetchable again.
 */
@Service
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);

    @Value("${nylepay.checkout.domain:https://nyle-pay.onrender.com}")
    private String checkoutDomain;

    private final MerchantRepository merchantRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final EncryptionUtils encryptionUtils;

    public MerchantService(MerchantRepository merchantRepository,
                           CheckoutSessionRepository checkoutSessionRepository,
                           EncryptionUtils encryptionUtils) {
        this.merchantRepository      = merchantRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.encryptionUtils         = encryptionUtils;
    }

    /**
     * Registers a NylePay user as a merchant.
     * Returns the plaintext secretKey ONCE at registration — store it safely.
     */
    @Transactional
    public Map<String, Object> registerMerchant(Long userId, String businessName,
                                                 String businessEmail, String webhookUrl) {
        if (merchantRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("User already has a merchant account");
        }

        String publicKey    = "npy_pub_" + UUID.randomUUID().toString().replace("-", "");
        String rawSecretKey = "npy_sec_" + UUID.randomUUID().toString().replace("-", "");
        String webhookSecret = UUID.randomUUID().toString().replace("-", "");

        String encryptedSecretKey;
        try {
            encryptedSecretKey = encryptionUtils.encrypt(rawSecretKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt merchant secret key", e);
        }

        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        merchant.setBusinessName(businessName);
        merchant.setBusinessEmail(businessEmail);
        merchant.setWebhookUrl(webhookUrl);
        merchant.setWebhookSecret(webhookSecret);
        merchant.setPublicKey(publicKey);
        merchant.setEncryptedSecretKey(encryptedSecretKey);
        merchant.setStatus("PENDING"); // becomes ACTIVE after KYC
        merchantRepository.save(merchant);

        log.info("Merchant registered: userId={} businessName={}", userId, businessName);

        return Map.of(
            "merchantId",    merchant.getId(),
            "publicKey",     publicKey,
            "secretKey",     rawSecretKey,   // shown ONCE — user must save this
            "webhookSecret", webhookSecret,  // used to verify our webhooks to them
            "status",        "PENDING",
            "message",       "Keep your secretKey safe. It will not be shown again."
        );
    }

    /**
     * Issues a real sandbox API credential set for the signed-in developer.
     * Sandbox merchants authenticate against the same /api/v1/merchant endpoints,
     * but controllers must simulate money movement while status remains SANDBOX.
     */
    @Transactional
    public Map<String, Object> getOrCreateSandboxWorkspace(Long userId, String email) {
        return merchantRepository.findByUserId(userId)
                .map(merchant -> merchantKeyPayload(merchant, "SANDBOX".equalsIgnoreCase(merchant.getStatus())))
                .orElseGet(() -> {
                    String publicKey = "npy_test_pk_" + UUID.randomUUID().toString().replace("-", "");
                    String rawSecretKey = "npy_test_sk_" + UUID.randomUUID().toString().replace("-", "");
                    String webhookSecret = "npy_test_whsec_" + UUID.randomUUID().toString().replace("-", "");

                    Merchant merchant = new Merchant();
                    merchant.setUserId(userId);
                    merchant.setBusinessName("Sandbox Workspace");
                    merchant.setBusinessEmail(email);
                    merchant.setPublicKey(publicKey);
                    merchant.setEncryptedSecretKey(encryptionUtils.encrypt(rawSecretKey));
                    merchant.setWebhookSecret(webhookSecret);
                    merchant.setStatus("SANDBOX");
                    merchant.setKycStatus("NONE");
                    merchantRepository.save(merchant);

                    log.info("Sandbox merchant workspace created: userId={}", userId);
                    return merchantKeyPayload(merchant, true);
                });
    }

    /**
     * Upgrades an existing sandbox workspace into a pending production profile,
     * or creates a pending merchant profile for users who skipped sandbox first.
     */
    @Transactional
    public Map<String, Object> registerOrUpdateBusiness(Long userId, String businessName,
                                                        String businessEmail, String webhookUrl,
                                                        String settlementMethod,
                                                        String settlementPhone,
                                                        String bankName,
                                                        String bankAccount) {
        if (businessName == null || businessName.isBlank()) {
            throw new RuntimeException("businessName is required");
        }

        Merchant merchant = merchantRepository.findByUserId(userId).orElse(null);
        String rawSecretKey = null;

        if (merchant == null) {
            rawSecretKey = "npy_sec_" + UUID.randomUUID().toString().replace("-", "");
            merchant = new Merchant();
            merchant.setUserId(userId);
            merchant.setPublicKey("npy_pub_" + UUID.randomUUID().toString().replace("-", ""));
            merchant.setEncryptedSecretKey(encryptionUtils.encrypt(rawSecretKey));
            merchant.setWebhookSecret(UUID.randomUUID().toString().replace("-", ""));
        } else if (merchant.getPublicKey() != null && merchant.getPublicKey().startsWith("npy_test_pk_")) {
            rawSecretKey = "npy_sec_" + UUID.randomUUID().toString().replace("-", "");
            merchant.setPublicKey("npy_pub_" + UUID.randomUUID().toString().replace("-", ""));
            merchant.setEncryptedSecretKey(encryptionUtils.encrypt(rawSecretKey));
            merchant.setWebhookSecret(UUID.randomUUID().toString().replace("-", ""));
        }

        merchant.setBusinessName(businessName);
        merchant.setBusinessEmail(businessEmail);
        merchant.setWebhookUrl(webhookUrl);
        merchant.setStatus("PENDING");
        merchant.setKycStatus("PENDING");
        if (settlementPhone != null && !settlementPhone.isBlank()) {
            merchant.setSettlementPhone(settlementPhone);
        }
        if (bankName != null && !bankName.isBlank()) {
            merchant.setSettlementBankName(bankName);
        }
        if (bankAccount != null && !bankAccount.isBlank()) {
            merchant.setSettlementBankAccount(bankAccount);
        }
        merchantRepository.save(merchant);

        Map<String, Object> payload = merchantKeyPayload(merchant, rawSecretKey != null);
        payload.put("settlementMethod", settlementMethod != null ? settlementMethod : "MPESA");
        payload.put("message", rawSecretKey != null
                ? "Production credentials generated. Keep the secret key safe."
                : "Production activation profile updated.");
        return payload;
    }

    private Map<String, Object> merchantKeyPayload(Merchant merchant, boolean secretVisible) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchantId", merchant.getId());
        payload.put("businessId", merchant.getId());
        payload.put("publicKey", merchant.getPublicKey());
        payload.put("status", merchant.getStatus());
        payload.put("kycStatus", merchant.getKycStatus());
        payload.put("mode", "SANDBOX".equalsIgnoreCase(merchant.getStatus()) ? "SANDBOX" : "LIVE");
        payload.put("hasSecretKey", merchant.getEncryptedSecretKey() != null);
        payload.put("hasWebhookSecret", merchant.getWebhookSecret() != null);
        if (secretVisible) {
            payload.put("secretKey", encryptionUtils.decrypt(merchant.getEncryptedSecretKey()));
            payload.put("webhookSecret", merchant.getWebhookSecret());
        }
        return payload;
    }

    /**
     * Creates a payment link (checkout session) that a merchant can share with customers.
     *
     * @param expiryMinutes  how long the link is valid (default 60)
     */
    @Transactional
    public Map<String, Object> createPaymentLink(
            Long merchantId,
            BigDecimal amount,
            String currency,
            String description,
            String redirectUrl,
            int expiryMinutes) {

        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new RuntimeException("Merchant not found"));

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new RuntimeException("Merchant account is not active. Complete KYC to activate.");
        }

        String ref = "NPY-LNK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        CheckoutSession session = new CheckoutSession();
        session.setMerchantId(merchantId);
        session.setAmount(amount);
        session.setCurrency(currency);
        session.setDescription(description);
        session.setReference(ref);
        session.setRedirectUrl(redirectUrl);
        session.setCallbackUrl(merchant.getWebhookUrl());
        session.setStatus("PENDING");
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        checkoutSessionRepository.save(session);

        String paymentUrl = checkoutDomain + "/checkout/" + ref;
        log.info("Payment link created: merchantId={} ref={} amount={} {}", merchantId, ref, amount, currency);

        return Map.of(
            "sessionId",   session.getId(),
            "reference",   ref,
            "paymentUrl",  paymentUrl,
            "amount",      amount,
            "currency",    currency,
            "expiresAt",   session.getExpiresAt().toString()
        );
    }

    public Merchant getMerchant(Long merchantId) {
        return merchantRepository.findById(merchantId)
            .orElseThrow(() -> new RuntimeException("Merchant not found"));
    }

    public Merchant getMerchantByUserId(Long userId) {
        return merchantRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("No merchant account for this user"));
    }

    public List<CheckoutSession> getMerchantSessions(Long merchantId) {
        return checkoutSessionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    @Transactional
    public Merchant saveMerchant(Merchant merchant) {
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant activateMerchant(Long merchantId) {
        Merchant merchant = getMerchant(merchantId);
        merchant.setStatus("ACTIVE");
        return merchantRepository.save(merchant);
    }
}
