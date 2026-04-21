package com.nyle.nylepay.services;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MpesaServiceTest {

    private final MpesaService mpesaService = new MpesaService();

    @Test
    void normalizePhoneNumber_acceptsCommonKenyanFormats() {
        assertEquals("254712345678", mpesaService.normalizePhoneNumber("0712345678"));
        assertEquals("254712345678", mpesaService.normalizePhoneNumber("712345678"));
        assertEquals("254712345678", mpesaService.normalizePhoneNumber("+254712345678"));
        assertEquals("254712345678", mpesaService.normalizePhoneNumber("254712345678"));
    }

    @Test
    void extractTransactionDetails_parsesNestedStkCallbackPayload() {
        Map<String, Object> payload = Map.of(
                "Body", Map.of(
                        "stkCallback", Map.of(
                                "MerchantRequestID", "29115-34620561-1",
                                "CheckoutRequestID", "ws_CO_191220191020363925",
                                "ResultCode", 0,
                                "ResultDesc", "The service request is processed successfully.",
                                "CallbackMetadata", Map.of(
                                        "Item", List.of(
                                                Map.of("Name", "Amount", "Value", 1000),
                                                Map.of("Name", "MpesaReceiptNumber", "Value", "NLJ7RT61SV"),
                                                Map.of("Name", "PhoneNumber", "Value", 254712345678L)
                                        )))));

        Map<String, Object> details = mpesaService.extractTransactionDetails(payload);

        assertEquals("ws_CO_191220191020363925", details.get("CheckoutRequestID"));
        assertEquals("NLJ7RT61SV", details.get("MpesaReceiptNumber"));
        assertEquals(1000, details.get("Amount"));
        assertEquals(254712345678L, details.get("PhoneNumber"));
        assertTrue(mpesaService.validateCallback(payload));
    }

    @Test
    void extractDisbursementDetails_parsesResultParameters() {
        Map<String, Object> payload = Map.of(
                "Result", Map.of(
                        "ConversationID", "AG_20260419_123456",
                        "OriginatorConversationID", "12345-67890-1",
                        "ResultCode", 0,
                        "ResultDesc", "The service request is processed successfully.",
                        "ResultParameters", Map.of(
                                "ResultParameter", List.of(
                                        Map.of("Key", "TransactionAmount", "Value", 500),
                                        Map.of("Key", "TransactionReceipt", "Value", "QWE123XYZ")
                                ))));

        Map<String, Object> details = mpesaService.extractDisbursementDetails(payload);

        assertEquals("AG_20260419_123456", details.get("ConversationID"));
        assertEquals("QWE123XYZ", details.get("TransactionReceipt"));
        assertEquals(500, details.get("TransactionAmount"));
    }
}
