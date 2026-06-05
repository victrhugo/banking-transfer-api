package com.compassuol.bank.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "O id da conta de origem é obrigatório")
        UUID senderId,
        @NotNull(message = "O id da conta de destino é obrigatório")
        UUID receiverId,
        @NotNull(message = "O valor da transferência é obrigatório")
        @Positive(message = "O valor da transferência deve ser maior que zero")
        BigDecimal amount) {
}
