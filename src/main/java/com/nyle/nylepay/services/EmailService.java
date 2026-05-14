package com.nyle.nylepay.services;

import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    // WELCOME EMAIL
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to NylePay! 🎉";
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;background:#0f172a;border-radius:16px;overflow:hidden">
              <div style="background:linear-gradient(135deg,#2563eb,#06b6d4);padding:40px 32px;text-align:center">
                <h1 style="color:#fff;margin:0;font-size:28px">Welcome to NylePay</h1>
                <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:16px">Your gateway to seamless payments</p>
              </div>
              <div style="padding:32px;color:#e2e8f0">
                <p style="font-size:18px;margin:0 0 16px">Hi <strong>%s</strong>,</p>
                <p style="line-height:1.7;margin:0 0 24px">
                  Your NylePay account is ready! You can now send money via M-Pesa, link bank accounts,
                  and trade crypto — all from one wallet.
                </p>
                <div style="text-align:center;margin:32px 0">
                  <a href="https://nylepay.com" style="background:#2563eb;color:#fff;padding:14px 32px;border-radius:9999px;text-decoration:none;font-weight:600;font-size:15px">
                    Open NylePay
                  </a>
                </div>
                <p style="color:#94a3b8;font-size:13px;margin:24px 0 0;text-align:center">
                  If you didn't create this account, please ignore this email.
                </p>
              </div>
            </div>
            """.formatted(user.getFullName());

        send(user.getEmail(), subject, html);
    }

    // TRANSACTION NOTIFICATION
    public void sendTransactionNotification(User user, Transaction transaction) {
        String statusColor = "COMPLETED".equals(transaction.getStatus()) ? "#22c55e" : 
                             "FAILED".equals(transaction.getStatus()) ? "#ef4444" : "#eab308";
        String statusLabel = transaction.getStatus();
        String type = transaction.getType();
        BigDecimal amount = transaction.getAmount();
        String currency = transaction.getCurrency();

        String subject = "NylePay — " + type + " " + statusLabel;
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;background:#0f172a;border-radius:16px;overflow:hidden">
              <div style="background:linear-gradient(135deg,#2563eb,#06b6d4);padding:32px;text-align:center">
                <h1 style="color:#fff;margin:0;font-size:24px">Transaction Update</h1>
              </div>
              <div style="padding:32px;color:#e2e8f0">
                <p style="font-size:16px;margin:0 0 24px">Hi <strong>%s</strong>,</p>
                <div style="background:#1e293b;border-radius:12px;padding:24px;margin:0 0 24px">
                  <table style="width:100%%;border-collapse:collapse">
                    <tr><td style="color:#94a3b8;padding:8px 0">Type</td><td style="text-align:right;font-weight:600">%s</td></tr>
                    <tr><td style="color:#94a3b8;padding:8px 0">Amount</td><td style="text-align:right;font-weight:600">%s %s</td></tr>
                    <tr><td style="color:#94a3b8;padding:8px 0">Status</td><td style="text-align:right"><span style="background:%s;color:#fff;padding:4px 12px;border-radius:9999px;font-size:13px;font-weight:600">%s</span></td></tr>
                    <tr><td style="color:#94a3b8;padding:8px 0">Provider</td><td style="text-align:right;font-weight:600">%s</td></tr>
                  </table>
                </div>
                <p style="color:#94a3b8;font-size:13px;text-align:center">
                  Transaction ID: %s
                </p>
              </div>
            </div>
            """.formatted(
                user.getFullName(), type, amount, currency,
                statusColor, statusLabel,
                transaction.getProvider() != null ? transaction.getProvider() : "—",
                transaction.getId()
            );

        send(user.getEmail(), subject, html);
    }

    // PASSWORD RESET
    public void sendPasswordResetEmail(User user, String resetToken) {
        String resetUrl = "https://nylepay.com/reset-password?token=" + resetToken;
        String subject = "NylePay — Reset Your Password";
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;background:#0f172a;border-radius:16px;overflow:hidden">
              <div style="background:linear-gradient(135deg,#2563eb,#06b6d4);padding:32px;text-align:center">
                <h1 style="color:#fff;margin:0;font-size:24px">Password Reset</h1>
              </div>
              <div style="padding:32px;color:#e2e8f0">
                <p style="font-size:16px;margin:0 0 16px">Hi <strong>%s</strong>,</p>
                <p style="line-height:1.7;margin:0 0 24px">
                  We received a request to reset your NylePay password. Click the button below to set a new password.
                  This link expires in 1 hour.
                </p>
                <div style="text-align:center;margin:32px 0">
                  <a href="%s" style="background:#2563eb;color:#fff;padding:14px 32px;border-radius:9999px;text-decoration:none;font-weight:600;font-size:15px">
                    Reset Password
                  </a>
                </div>
                <p style="color:#94a3b8;font-size:13px;margin:24px 0 0;text-align:center">
                  If you didn't request this, you can safely ignore this email.
                </p>
              </div>
            </div>
            """.formatted(user.getFullName(), resetUrl);

        send(user.getEmail(), subject, html);
    }

    public void sendBusinessAccessVerificationEmail(User user, String verificationUrl) {
        String subject = "Confirm your NylePay Business email";
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;background:#ffffff;border:1px solid #dbe7ff;border-radius:12px;overflow:hidden">
              <div style="background:#0f4fd8;padding:30px 28px">
                <h1 style="color:#fff;margin:0;font-size:24px">Confirm NylePay Business access</h1>
                <p style="color:rgba(255,255,255,0.82);margin:8px 0 0;font-size:14px">Secure access for routing, sandbox keys, and business setup.</p>
              </div>
              <div style="padding:28px;color:#172033">
                <p style="font-size:16px;margin:0 0 16px">Hi <strong>%s</strong>,</p>
                <p style="line-height:1.7;margin:0 0 24px;color:#475569">
                  Confirm this email address to open your NylePay Business workspace.
                </p>
                <div style="margin:28px 0">
                  <a href="%s" style="display:inline-block;background:#1769e0;color:#fff;padding:13px 24px;border-radius:8px;text-decoration:none;font-weight:700">
                    Confirm email
                  </a>
                </div>
                <p style="color:#64748b;font-size:13px;line-height:1.6;margin:0">
                  This link expires in 24 hours. If you did not request NylePay Business access, you can ignore this email.
                </p>
              </div>
            </div>
            """.formatted(user.getFullName(), verificationUrl);

        send(user.getEmail(), subject, html);
    }

    // GENERIC EMAIL (used by OtpService and others)
    public void sendGenericEmail(String to, String subject, String textBody) {
        String html = """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;background:#0f172a;border-radius:16px;overflow:hidden">
              <div style="background:linear-gradient(135deg,#2563eb,#06b6d4);padding:32px;text-align:center">
                <h1 style="color:#fff;margin:0;font-size:24px">NylePay</h1>
              </div>
              <div style="padding:32px;color:#e2e8f0">
                <pre style="font-family:'Segoe UI',Arial,sans-serif;white-space:pre-wrap;line-height:1.7;margin:0">%s</pre>
              </div>
            </div>
            """.formatted(textBody.replace("<", "&lt;").replace(">", "&gt;"));

        send(to, subject, html);
    }

    // CORE SENDER (Resend REST API)
    private void send(String to, String subject, String htmlBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", new String[]{to},
                "subject", subject,
                "html", htmlBody
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                RESEND_API_URL,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Email sent successfully to {} — subject: {}", to, subject);
            } else {
                logger.warn("Resend API returned status {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            // Non-blocking: email failure should not crash the main flow
        }
    }
}
