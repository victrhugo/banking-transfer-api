package com.compassuol.bank.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "O nome da conta é obrigatório")
        String name,
        @NotNull(message = "O saldo inicial é obrigatório")
        @PositiveOrZero(message = "O saldo inicial não pode ser negativo")
        BigDecimal initialBalance) {
}
