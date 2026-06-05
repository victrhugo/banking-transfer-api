package com.compassuol.bank.transfer.controller;

import com.compassuol.bank.transfer.dto.TransferRequest;
import com.compassuol.bank.transfer.dto.TransferResponse;
import com.compassuol.bank.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "Executar transferência entre contas")
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.makeTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar transferências de uma conta")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransferResponse>> getTransfersByAccount(@PathVariable("accountId") UUID accountId) {
        return ResponseEntity.ok(transferService.getTransfersByAccount(accountId));
    }
}
