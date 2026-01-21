package com.nyle.nylepay.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "payment_methods")
@Data
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String type;      // MPESA, BANK, CRYPTO
    private String details;   // JSON string
    private boolean isDefault;
    private boolean isVerified = false;
}

