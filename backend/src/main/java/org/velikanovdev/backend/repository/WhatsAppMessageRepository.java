package org.velikanovdev.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.velikanovdev.backend.entity.WhatsAppMessage;

import java.util.Date;
import java.util.List;

@Repository
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    List<WhatsAppMessage> findBySenderAndSentDateAfter(String sender, Date sentDate);
    List<WhatsAppMessage> findBySenderAndUnreadTrue(String sender);

    @Query("SELECT COUNT(m) FROM WhatsAppMessage m WHERE m.sender = :sender AND m.unread = true")
    long countUnreadMessagesBySender(@Param("sender") String sender);

    List<WhatsAppMessage> findByUnreadTrue();
}

