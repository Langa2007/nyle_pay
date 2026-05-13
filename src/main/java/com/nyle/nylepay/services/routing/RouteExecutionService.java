package com.nyle.nylepay.services.routing;

import com.nyle.nylepay.dto.RouteRequest;
import com.nyle.nylepay.models.Transaction;
import com.nyle.nylepay.services.MpesaService;
import com.nyle.nylepay.services.TransactionService;
import com.nyle.nylepay.services.WalletService;
import com.nyle.nylepay.services.cex.CexRoutingService;
import com.nyle.nylepay.services.chain.OnChainWithdrawalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class RouteExecutionService {

    private final RouteQuoteService quoteService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final MpesaService mpesaService;
    private final CexRoutingService cexRoutingService;
    private final OnChainWithdrawalService onChainWithdrawalService;

    public RouteExecutionService(RouteQuoteService quoteService,
            TransactionService transactionService,
            WalletService walletService,
            MpesaService mpesaService,
            CexRoutingService cexRoutingService,
            OnChainWithdrawalService onChainWithdrawalService) {
        this.quoteService = quoteService;
        this.transactionService = transactionService;
        this.walletService = walletService;
        this.mpesaService = mpesaService;
        this.cexRoutingService = cexRoutingService;
        this.onChainWithdrawalService = onChainWithdrawalService;
    }

    @Transactional
    public Map<String, Object> execute(Long userId, RouteRequest request) {
        String sourceRail = quoteService.normalizeRail(request.getSourceRail());
        String destinationRail = quoteService.normalizeRail(request.getDestinationRail());
        String sourceAsset = quoteService.normalizeAsset(request.getSourceAsset());
        String destinationAsset = quoteService.normalizeAsset(
                request.getDestinationAsset() != null ? request.getDestinationAsset() : request.getSourceAsset());

        Map<String, Object> quote = quoteService.quote(request);

        if ("NYLEPAY_WALLET".equals(sourceRail)) {
            return executeWalletFundedRoute(userId, request, destinationRail, sourceAsset, destinationAsset, quote);
        }
        if ("MPESA".equals(sourceRail)) {
            return initiateMpesaInbound(userId, request, destinationRail, destinationAsset, quote);
        }
        if ("ONCHAIN".equals(sourceRail)) {
            return initiateOnChainInbound(userId, request, destinationRail, destinationAsset, quote);
        }
        if ("CEX".equals(sourceRail)) {
            return executeCexRoute(userId, request, destinationRail, quote);
        }

        return Map.of(
                "status", "ROUTE_NOT_AUTOMATED",
                "quote", quote,
                "message", "This rail pair is quoted but not automated yet. Use the route quote to select another route.");
    }

    private Map<String, Object> executeWalletFundedRoute(Long userId, RouteRequest request,
            String destinationRail, String sourceAsset, String destinationAsset, Map<String, Object> quote) {

        if (!sourceAsset.equals(destinationAsset)) {
            Transaction conversion = transactionService.createConversion(
                    userId, sourceAsset, destinationAsset, request.getAmount());
            return routeResult("CONVERSION_COMPLETED", conversion, quote,
                    "Funds converted inside NylePay wallet. Execute a second route to settle externally.");
        }

        switch (destinationRail) {
            case "MPESA":
                return withdrawToMpesa(userId, request, sourceAsset, quote);
            case "BANK":
                return withdrawToBank(userId, request, sourceAsset, quote);
            case "TILL":
            case "PAYBILL":
            case "POCHI":
                return payLocalMerchant(userId, request, destinationRail, quote);
            case "NYLEPAY_WALLET":
                return transferToNylePayAccount(userId, request, sourceAsset, quote);
            case "ONCHAIN":
                return withdrawOnChain(userId, request, sourceAsset, quote);
            default:
                return Map.of(
                        "status", "ROUTE_NOT_AUTOMATED",
                        "quote", quote,
                        "message", "Wallet-funded route to " + destinationRail + " is not automated yet.");
        }
    }

    private Map<String, Object> initiateMpesaInbound(Long userId, RouteRequest request,
            String destinationRail, String destinationAsset, Map<String, Object> quote) {
        if (!"NYLEPAY_WALLET".equals(destinationRail) && !"MERCHANT".equals(destinationRail)) {
            throw new IllegalArgumentException("M-Pesa source routes must first settle into NylePay wallet or merchant.");
        }
        String phone = requiredDestination(request, "phone");
        String reference = firstNonBlank(request.getIdempotencyKey(), "ROUTE_MPESA_" + System.currentTimeMillis());
        Map<String, Object> mpesaResponse = mpesaService.stkPush(phone, request.getAmount(), reference);
        Transaction tx = transactionService.createDeposit(
                userId, "MPESA", request.getAmount(), destinationAsset,
                asString(mpesaResponse.get("CheckoutRequestID")), buildMetadata("MPESA", destinationRail, request));
        return routeResult("INTAKE_INITIATED", tx, quote,
                "M-Pesa STK sent. NylePay will continue the route after Safaricom confirms payment.");
    }

    private Map<String, Object> initiateOnChainInbound(Long userId, RouteRequest request,
            String destinationRail, String destinationAsset, Map<String, Object> quote) {
        if (!"NYLEPAY_WALLET".equals(destinationRail) && !"MPESA".equals(destinationRail)
                && !"BANK".equals(destinationRail) && !"MERCHANT".equals(destinationRail)) {
            throw new IllegalArgumentException("Unsupported on-chain destination: " + destinationRail);
        }
        try {
            Map<String, Object> wallet = walletService.createCryptoWallet(userId);
            return Map.of(
                    "status", "INTAKE_REQUIRED",
                    "quote", quote,
                    "depositAddress", wallet.get("address"),
                    "chains", wallet.get("chains"),
                    "asset", quoteService.normalizeAsset(request.getSourceAsset()),
                    "destinationRail", destinationRail,
                    "message", "Send the crypto deposit to this NylePay custody address. The route continues after webhook confirmations.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to prepare custody address: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> executeCexRoute(Long userId, RouteRequest request,
            String destinationRail, Map<String, Object> quote) {
        if (!"MPESA".equals(destinationRail)) {
            return Map.of(
                    "status", "LIQUIDITY_ROUTE_NOT_AUTOMATED",
                    "quote", quote,
                    "message", "CEX routes currently automate MPESA payout preparation first. Bank and merchant CEX settlement need a liquidity adapter.");
        }
        String phone = requiredDestination(request, "phone");
        Map<String, Object> result = cexRoutingService.autoRouteToMpesa(
                userId, quoteService.normalizeAsset(request.getSourceAsset()), request.getAmount(), phone);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "LIQUIDITY_ROUTE_PREPARED");
        response.put("quote", quote);
        response.put("providerResult", result);
        response.put("message", "CEX asset route prepared. Production payout requires institutional liquidity and callback-confirmed settlement.");
        return response;
    }

    private Map<String, Object> withdrawToMpesa(Long userId, RouteRequest request,
            String asset, Map<String, Object> quote) {
        String phone = mpesaService.normalizePhoneNumber(requiredDestination(request, "phone"));
        Transaction tx = transactionService.createWithdrawal(userId, "MPESA", request.getAmount(), asset, phone);
        return routeResult("PROCESSING", tx, quote,
                "Wallet debited and M-Pesa payout dispatched. Final status is updated by callback.");
    }

    private Map<String, Object> withdrawToBank(Long userId, RouteRequest request,
            String asset, Map<String, Object> quote) {
        String accountNumber = requiredDestination(request, "accountNumber");
        String bankCode = requiredDestination(request, "bankCode");
        String country = firstNonBlank(request.getDestination().get("country"), request.getCountry(), "KE");
        String accountName = firstNonBlank(request.getDestination().get("accountName"), "");
        String destination = accountNumber + "|" + bankCode + "|" + country + "|" + accountName;
        Transaction tx = transactionService.createWithdrawal(userId, "BANK", request.getAmount(), asset, destination);
        return routeResult("PROCESSING", tx, quote,
                "Wallet debited and bank payout dispatched. Final status is updated by provider callback.");
    }

    private Map<String, Object> payLocalMerchant(Long userId, RouteRequest request,
            String destinationRail, Map<String, Object> quote) {
        String destination;
        String accountRef = null;
        Map<String, Object> mpesaResponse;

        walletService.subtractBalance(userId, "KSH", request.getAmount());
        if ("TILL".equals(destinationRail)) {
            destination = requiredDestination(request, "tillNumber");
            mpesaResponse = mpesaService.payToTill(destination, request.getAmount(), request.getPurpose());
        } else if ("PAYBILL".equals(destinationRail)) {
            destination = requiredDestination(request, "paybillNumber");
            accountRef = firstNonBlank(request.getDestination().get("accountNumber"), request.getDestination().get("accountRef"));
            mpesaResponse = mpesaService.payToPaybill(destination, accountRef, request.getAmount(), request.getPurpose());
        } else {
            destination = mpesaService.normalizePhoneNumber(requiredDestination(request, "phone"));
            accountRef = destination;
            mpesaResponse = mpesaService.payToPochi(destination, request.getAmount(), request.getPurpose());
        }

        Transaction tx = transactionService.createLocalPayment(
                userId, destinationRail, request.getAmount(), destination, accountRef, mpesaResponse);
        return routeResult("PROCESSING", tx, quote,
                "Local M-Pesa payment dispatched. Final status is updated by B2B callback.");
    }

    private Map<String, Object> transferToNylePayAccount(Long userId, RouteRequest request,
            String asset, Map<String, Object> quote) {
        String accountNumber = requiredDestination(request, "accountNumber");
        Transaction tx = transactionService.createTransfer(
                userId, accountNumber, request.getAmount(), asset, request.getPurpose());
        return routeResult("COMPLETED", tx, quote, "NylePay wallet transfer completed.");
    }

    private Map<String, Object> withdrawOnChain(Long userId, RouteRequest request,
            String asset, Map<String, Object> quote) {
        String address = requiredDestination(request, "address");
        String chain = firstNonBlank(request.getDestination().get("chain"), "POLYGON");
        Transaction tx = onChainWithdrawalService.initiateWithdrawal(
                userId, asset, request.getAmount(), chain, "WALLET", address);
        return routeResult("PENDING_APPROVAL", tx, quote,
                "On-chain withdrawal reserved. Call the on-chain confirmation flow to broadcast.");
    }

    private Map<String, Object> routeResult(String status, Transaction tx,
            Map<String, Object> quote, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("transactionId", tx.getId());
        response.put("transactionCode", tx.getTransactionCode());
        response.put("provider", tx.getProvider());
        response.put("transactionStatus", tx.getStatus());
        response.put("quote", quote);
        response.put("message", message);
        return response;
    }

    private String requiredDestination(RouteRequest request, String key) {
        String value = request.getDestination().get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("destination." + key + " is required for this route");
        }
        return value.trim();
    }

    private String buildMetadata(String sourceRail, String destinationRail, RouteRequest request) {
        return "{\"route\":\"" + sourceRail + "_TO_" + destinationRail + "\",\"purpose\":\""
                + safe(request.getPurpose()) + "\"}";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}
