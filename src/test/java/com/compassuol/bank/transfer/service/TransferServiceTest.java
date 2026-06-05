package com.compassuol.bank.transfer.service;

import com.compassuol.bank.account.entity.Account;
import com.compassuol.bank.account.repository.AccountRepository;
import com.compassuol.bank.common.exception.AccountNotFoundException;
import com.compassuol.bank.common.exception.InsufficientBalanceException;
import com.compassuol.bank.common.exception.InvalidTransferException;
import com.compassuol.bank.notification.service.NotificationService;
import com.compassuol.bank.transfer.dto.TransferRequest;
import com.compassuol.bank.transfer.entity.Transfer;
import com.compassuol.bank.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransferService transferService;

    private UUID senderId;
    private UUID receiverId;
    private Account sender;
    private Account receiver;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
        sender = Account.builder().id(senderId).name("Sender").balance(new BigDecimal("300.00")).build();
        receiver = Account.builder().id(receiverId).name("Receiver").balance(new BigDecimal("100.00")).build();
    }

    @Test
    void shouldTransferSuccessfully() {
        when(accountRepository.findByIdForUpdate(senderId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(receiverId)).thenReturn(Optional.of(receiver));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> transferService.makeTransfer(new TransferRequest(senderId, receiverId, new BigDecimal("150.00"))));

        verify(accountRepository, times(1)).save(sender);
        verify(accountRepository, times(1)).save(receiver);
        verify(transferRepository, times(1)).save(any(Transfer.class));
        verify(notificationService, times(1)).notifyTransferSuccess(receiverId);
    }

    @Test
    void shouldFailWhenSenderNotFound() {
        when(accountRepository.findByIdForUpdate(senderId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> transferService.makeTransfer(new TransferRequest(senderId, receiverId, new BigDecimal("100.00"))));

        verify(accountRepository, never()).save(any());
        verify(notificationService, never()).notifyTransferSuccess(any());
    }

    @Test
    void shouldFailWhenReceiverNotFound() {
        when(accountRepository.findByIdForUpdate(senderId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(receiverId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> transferService.makeTransfer(new TransferRequest(senderId, receiverId, new BigDecimal("100.00"))));

        verify(accountRepository, never()).save(receiver);
        verify(notificationService, never()).notifyTransferSuccess(any());
    }

    @Test
    void shouldFailWhenInsufficientBalance() {
        when(accountRepository.findByIdForUpdate(senderId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(receiverId)).thenReturn(Optional.of(receiver));

        assertThrows(InsufficientBalanceException.class,
                () -> transferService.makeTransfer(new TransferRequest(senderId, receiverId, new BigDecimal("500.00"))));

        verify(transferRepository, never()).save(any());
        verify(notificationService, never()).notifyTransferSuccess(any());
    }

    @Test
    void shouldFailWhenSameAccount() {
        assertThrows(InvalidTransferException.class,
                () -> transferService.makeTransfer(new TransferRequest(senderId, senderId, new BigDecimal("50.00"))));

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldFailWhenAmountIsZero() {
        assertThrows(InvalidTransferException.class,
                () -> transferService.makeTransfer(new TransferRequest(senderId, receiverId, BigDecimal.ZERO)));

        verify(accountRepository, never()).findByIdForUpdate(any());
    }
}
