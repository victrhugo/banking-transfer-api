package com.compassuol.bank.transfer.service;

import com.compassuol.bank.account.entity.Account;
import com.compassuol.bank.account.repository.AccountRepository;
import com.compassuol.bank.common.exception.AccountNotFoundException;
import com.compassuol.bank.common.exception.InsufficientBalanceException;
import com.compassuol.bank.common.exception.InvalidTransferException;
import com.compassuol.bank.notification.service.NotificationService;
import com.compassuol.bank.transfer.dto.TransferRequest;
import com.compassuol.bank.transfer.dto.TransferResponse;
import com.compassuol.bank.transfer.entity.Transfer;
import com.compassuol.bank.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final NotificationService notificationService;

    @Transactional
    public TransferResponse makeTransfer(TransferRequest request) {
        validateRequest(request);

        Account sender = accountRepository.findByIdForUpdate(request.senderId())
                .orElseThrow(() -> new AccountNotFoundException("Conta de origem não encontrada: " + request.senderId()));

        Account receiver = accountRepository.findByIdForUpdate(request.receiverId())
                .orElseThrow(() -> new AccountNotFoundException("Conta de destino não encontrada: " + request.receiverId()));

        if (sender.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente na conta de origem");
        }

        sender.debit(request.amount());
        receiver.credit(request.amount());

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transfer transfer = Transfer.builder()
                .senderId(sender.getId())
                .receiverId(receiver.getId())
                .amount(request.amount())
                .build();

        Transfer savedTransfer = transferRepository.save(transfer);
        notificationService.notifyTransferSuccess(receiver.getId());

        return toResponse(savedTransfer);
    }

    public List<TransferResponse> getTransfersByAccount(UUID accountId) {
        return transferRepository.findBySenderIdOrReceiverId(accountId, accountId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void validateRequest(TransferRequest request) {
        if (request.senderId().equals(request.receiverId())) {
            throw new InvalidTransferException("A transferência não pode ser realizada para a mesma conta");
        }

        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new InvalidTransferException("O valor da transferência deve ser maior que zero");
        }
    }

    private TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSenderId(),
                transfer.getReceiverId(),
                transfer.getAmount(),
                transfer.getCreatedAt());
    }
}
