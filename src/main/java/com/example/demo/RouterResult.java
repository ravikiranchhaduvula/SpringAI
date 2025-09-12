package com.example.demo;

import java.math.BigDecimal;

public record RouterResult(
        String action,    // e.g., "createPaymentHold" or null
        String answer,    // short reply or null
        String invoiceId, // required if action != null
        BigDecimal amount // required if action != null
) {}
