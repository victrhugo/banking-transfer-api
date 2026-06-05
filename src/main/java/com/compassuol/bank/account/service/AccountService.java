package com.compassuol.bank.account.service;

import com.compassuol.bank.account.dto.AccountResponse;
import com.compassuol.bank.account.dto.CreateAccountRequest;
import com.compassuol.bank.account.entity.Account;
import com.compassuol.bank.account.repository.AccountRepository;
import com.compassuol.bank.common.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .name(request.name().trim())
                .balance(request.initialBalance())
                .build();

        return toResponse(accountRepository.save(account));
    }

    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(this::toResponse)
                .orElseThrow(() -> new AccountNotFoundException("Conta não encontrada: " + accountId));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getBalance());
    }
}
