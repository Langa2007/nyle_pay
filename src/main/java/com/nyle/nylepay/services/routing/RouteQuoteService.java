package com.nyle.nylepay.services.routing;

import com.nyle.nylepay.dto.RouteRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteQuoteService {

    @Value("${merchant.fee-percent:1.5}")
    private BigDecimal merchantFeePercent;

    public Map<String, Object> quote(RouteRequest request) {
        String sourceRail = normalizeRail(request.getSourceRail());
        String destinationRail = normalizeRail(request.getDestinationRail());
        String sourceAsset = normalizeAsset(request.getSourceAsset());
        String destinationAsset = normalizeAsset(
                request.getDestinationAsset() != null ? request.getDestinationAsset() : request.getSourceAsset());
        String country = normalizeCountry(request.getCountry());

        validateKenyaFirst(sourceRail, destinationRail, country);

        BigDecimal fxRate = estimateRate(sourceAsset, destinationAsset);
        BigDecimal destinationAmount = request.getAmount().multiply(fxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nylePayFee = calculateFee(sourceRail, destinationRail, destinationAmount);
        BigDecimal networkFee = estimateNetworkFee(sourceRail, destinationRail, destinationAsset);
        BigDecimal netAmount = destinationAmount.subtract(nylePayFee).subtract(networkFee);
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            netAmount = BigDecimal.ZERO;
        }

        Map<String, Object> quote = new HashMap<>();
        quote.put("route", sourceRail + "_TO_" + destinationRail);
        quote.put("country", country);
        quote.put("sourceRail", sourceRail);
        quote.put("destinationRail", destinationRail);
        quote.put("sourceAsset", sourceAsset);
        quote.put("destinationAsset", destinationAsset);
        quote.put("amountIn", request.getAmount());
        quote.put("fxRate", fxRate);
        quote.put("grossAmountOut", destinationAmount);
        quote.put("nylePayFee", nylePayFee);
        quote.put("networkFeeEstimate", networkFee);
        quote.put("netAmountOut", netAmount);
        quote.put("estimatedSpeed", estimateSpeed(sourceRail, destinationRail));
        quote.put("settlementMode", settlementMode(sourceRail, destinationRail));
        quote.put("legs", buildLegs(sourceRail, destinationRail, sourceAsset, destinationAsset));
        quote.put("sandbox", true);
        quote.put("message", "Quote is indicative. Final provider fees and FX are locked when execution starts.");
        return quote;
    }

    private void validateKenyaFirst(String sourceRail, String destinationRail, String country) {
        if (!"KE".equals(country)) {
            throw new IllegalArgumentException("This routing engine is Kenya-first for now. Use country=KE.");
        }

        List<String> supportedRails = List.of(
                "NYLEPAY_WALLET", "MPESA", "AIRTEL_MONEY", "PESALINK", "BANK", "CARD", "ONCHAIN", "CEX",
                "TILL", "PAYBILL", "POCHI", "MERCHANT");
        if (!supportedRails.contains(sourceRail)) {
            throw new IllegalArgumentException("Unsupported sourceRail: " + sourceRail);
        }
        if (!supportedRails.contains(destinationRail)) {
            throw new IllegalArgumentException("Unsupported destinationRail: " + destinationRail);
        }
    }

    private BigDecimal calculateFee(String sourceRail, String destinationRail, BigDecimal amount) {
        BigDecimal percent = "MERCHANT".equals(destinationRail)
                ? merchantFeePercent
                : BigDecimal.valueOf(1.0);
        if ("NYLEPAY_WALLET".equals(sourceRail) && "NYLEPAY_WALLET".equals(destinationRail)) {
            percent = BigDecimal.valueOf(0.25);
        }
        if ("ONCHAIN".equals(sourceRail) || "ONCHAIN".equals(destinationRail) || "CEX".equals(sourceRail)) {
            percent = BigDecimal.valueOf(1.5);
        }
        return amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateNetworkFee(String sourceRail, String destinationRail, String asset) {
        if ("MPESA".equals(destinationRail) || "AIRTEL_MONEY".equals(destinationRail)
                || "TILL".equals(destinationRail)
                || "PAYBILL".equals(destinationRail) || "POCHI".equals(destinationRail)) {
            return BigDecimal.valueOf(5);
        }
        if ("PESALINK".equals(destinationRail)) {
            return BigDecimal.valueOf(25);
        }
        if ("BANK".equals(destinationRail)) {
            return BigDecimal.valueOf(30);
        }
        if ("ONCHAIN".equals(destinationRail) || "ONCHAIN".equals(sourceRail)) {
            return "KSH".equals(asset) || "KES".equals(asset) ? BigDecimal.valueOf(150) : BigDecimal.valueOf(1);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal estimateRate(String sourceAsset, String destinationAsset) {
        if (sourceAsset.equals(destinationAsset)) {
            return BigDecimal.ONE;
        }
        if (isKes(sourceAsset) && "USD".equals(destinationAsset)) {
            return BigDecimal.valueOf(0.0077);
        }
        if ("USD".equals(sourceAsset) && isKes(destinationAsset)) {
            return BigDecimal.valueOf(129.5);
        }
        if (List.of("USDT", "USDC", "DAI").contains(sourceAsset) && isKes(destinationAsset)) {
            return BigDecimal.valueOf(129.5);
        }
        if (isKes(sourceAsset) && List.of("USDT", "USDC").contains(destinationAsset)) {
            return BigDecimal.valueOf(0.0077);
        }
        if ("ETH".equals(sourceAsset) && isKes(destinationAsset)) {
            return BigDecimal.valueOf(450000);
        }
        return BigDecimal.ONE;
    }

    private List<Map<String, Object>> buildLegs(String sourceRail, String destinationRail,
            String sourceAsset, String destinationAsset) {
        List<Map<String, Object>> legs = new ArrayList<>();
        legs.add(Map.of("step", 1, "rail", sourceRail, "action", "collect", "asset", sourceAsset));
        if (!sourceAsset.equals(destinationAsset)) {
            legs.add(Map.of("step", legs.size() + 1, "rail", "NYLEPAY_ROUTER", "action", "convert",
                    "from", sourceAsset, "to", destinationAsset));
        }
        legs.add(Map.of("step", legs.size() + 1, "rail", destinationRail, "action", "settle", "asset",
                destinationAsset));
        return legs;
    }

    private String estimateSpeed(String sourceRail, String destinationRail) {
        if ("NYLEPAY_WALLET".equals(sourceRail) && "NYLEPAY_WALLET".equals(destinationRail)) {
            return "instant";
        }
        if ("ONCHAIN".equals(sourceRail) || "ONCHAIN".equals(destinationRail)) {
            return "5-30 minutes after confirmations";
        }
        if ("PESALINK".equals(destinationRail)) {
            return "near real-time bank switch settlement";
        }
        if ("AIRTEL_MONEY".equals(sourceRail) || "AIRTEL_MONEY".equals(destinationRail)) {
            return "near real-time after Airtel Money callback";
        }
        if ("BANK".equals(destinationRail)) {
            return "minutes to T+1 depending on bank/provider";
        }
        return "near real-time after provider callback";
    }

    private String settlementMode(String sourceRail, String destinationRail) {
        if ("NYLEPAY_WALLET".equals(sourceRail)) {
            return "wallet-funded";
        }
        if ("MPESA".equals(sourceRail) || "AIRTEL_MONEY".equals(sourceRail)
                || "PESALINK".equals(sourceRail) || "BANK".equals(sourceRail)
                || "CARD".equals(sourceRail) || "ONCHAIN".equals(sourceRail)) {
            return "inbound-confirmation-required";
        }
        if ("CEX".equals(sourceRail)) {
            return "liquidity-provider-required";
        }
        return "provider-confirmation-required";
    }

    private boolean isKes(String asset) {
        return "KES".equals(asset) || "KSH".equals(asset);
    }

    public String normalizeRail(String rail) {
        String normalized = rail == null ? "" : rail.trim().toUpperCase();
        if ("WALLET".equals(normalized)) {
            return "NYLEPAY_WALLET";
        }
        if ("AIRTEL".equals(normalized) || "AIRTELMONEY".equals(normalized)) {
            return "AIRTEL_MONEY";
        }
        if ("PESA_LINK".equals(normalized) || "IPSL".equals(normalized)) {
            return "PESALINK";
        }
        return normalized;
    }

    public String normalizeAsset(String asset) {
        String normalized = asset == null ? "KSH" : asset.trim().toUpperCase();
        return "KES".equals(normalized) ? "KSH" : normalized;
    }

    public String normalizeCountry(String country) {
        return country == null || country.isBlank() ? "KE" : country.trim().toUpperCase();
    }
}
