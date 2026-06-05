package com.compassuol.bank.transfer.repository;

import com.compassuol.bank.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    List<Transfer> findBySenderIdOrReceiverId(UUID senderId, UUID receiverId);
}
