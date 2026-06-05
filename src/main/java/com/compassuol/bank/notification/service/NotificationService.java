package com.compassuol.bank.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    public void notifyTransferSuccess(UUID receiverId) {
        log.info("Transferência realizada com sucesso para a conta {}", receiverId);
    }
}
