package com.compassuol.bank.transfer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID senderId,
        UUID receiverId,
        BigDecimal amount,
        LocalDateTime createdAt) {
}
