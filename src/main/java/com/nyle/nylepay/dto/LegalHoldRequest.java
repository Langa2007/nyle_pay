package com.nyle.nylepay.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class LegalHoldRequest {
    @NotBlank(message = "action is required")
    private String action;

    @NotBlank(message = "authority is required")
    private String authority;

    @NotBlank(message = "courtOrderReference is required")
    private String courtOrderReference;

    @NotBlank(message = "reason is required")
    private String reason;

    private LocalDateTime holdUntil;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getAuthority() { return authority; }
    public void setAuthority(String authority) { this.authority = authority; }
    public String getCourtOrderReference() { return courtOrderReference; }
    public void setCourtOrderReference(String courtOrderReference) { this.courtOrderReference = courtOrderReference; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getHoldUntil() { return holdUntil; }
    public void setHoldUntil(LocalDateTime holdUntil) { this.holdUntil = holdUntil; }
}
