package com.compassuol.bank.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, String name, BigDecimal balance) {
}
