package org.velikanovdev.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.velikanovdev.backend.entity.WhatsAppMessage;

import java.util.Date;
import java.util.List;

@Repository
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    List<WhatsAppMessage> findBySenderAndSentDateAfter(String sender, Date sentDate);
}

