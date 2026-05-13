package com.nyle.nylepay.dto;

import jakarta.validation.constraints.NotBlank;

public class ReversalResolutionRequest {
    @NotBlank(message = "recipientOutcome is required")
    private String recipientOutcome;

    private Long supportAgentUserId;
    private String notes;

    public String getRecipientOutcome() { return recipientOutcome; }
    public void setRecipientOutcome(String recipientOutcome) { this.recipientOutcome = recipientOutcome; }
    public Long getSupportAgentUserId() { return supportAgentUserId; }
    public void setSupportAgentUserId(Long supportAgentUserId) { this.supportAgentUserId = supportAgentUserId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
