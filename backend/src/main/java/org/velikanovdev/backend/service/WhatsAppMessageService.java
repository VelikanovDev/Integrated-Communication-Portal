package org.velikanovdev.backend.service;

import org.velikanovdev.backend.entity.WhatsAppConversation;
import org.velikanovdev.backend.entity.WhatsAppMessage;

import java.util.List;

public interface WhatsAppMessageService {
    WhatsAppMessage saveMessage(WhatsAppMessage message);

    List<WhatsAppConversation> getConversations();

    List<WhatsAppMessage> getMessages(String sender);

    boolean hasUserMessagedInLast24Hours(String sender);
}
