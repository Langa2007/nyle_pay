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

    @Value("${nylepay.checkout.domain:http://localhost:8080}")
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
