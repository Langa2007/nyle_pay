package com.nyle.nylepay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Universal money-routing request.
 *
 * The authenticated user owns the route. Do not trust a userId from the body for
 * money movement; controllers resolve the user from the JWT.
 */
public class RouteRequest {

    @NotBlank(message = "sourceRail is required")
    private String sourceRail;

    @NotBlank(message = "destinationRail is required")
    private String destinationRail;

    @NotBlank(message = "sourceAsset is required")
    private String sourceAsset;

    private String destinationAsset;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String country = "KE";
    private String purpose;
    private String idempotencyKey;
    private Map<String, String> destination = new HashMap<>();
    private Map<String, String> metadata = new HashMap<>();

    public String getSourceRail() { return sourceRail; }
    public void setSourceRail(String sourceRail) { this.sourceRail = sourceRail; }
    public String getDestinationRail() { return destinationRail; }
    public void setDestinationRail(String destinationRail) { this.destinationRail = destinationRail; }
    public String getSourceAsset() { return sourceAsset; }
    public void setSourceAsset(String sourceAsset) { this.sourceAsset = sourceAsset; }
    public String getDestinationAsset() { return destinationAsset; }
    public void setDestinationAsset(String destinationAsset) { this.destinationAsset = destinationAsset; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Map<String, String> getDestination() { return destination; }
    public void setDestination(Map<String, String> destination) {
        this.destination = destination != null ? destination : new HashMap<>();
    }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
}
