package org.velikanovdev.backend.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.velikanovdev.backend.entity.WhatsAppConversation;
import org.velikanovdev.backend.entity.WhatsAppMessage;
import org.velikanovdev.backend.repository.WhatsAppMessageRepository;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WhatsAppMessageServiceImpl implements WhatsAppMessageService {
    private final WhatsAppMessageRepository messageRepository;

    @Value("${whatsapp.phone.number}")
    private String PHONE_NUMBER;

    @Autowired
    public WhatsAppMessageServiceImpl(WhatsAppMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public WhatsAppMessage saveMessage(WhatsAppMessage message) {
        return messageRepository.save(message);
    }

    @Override
    public List<WhatsAppConversation> getConversations() {
        // Fetch all messages from the repository
        List<WhatsAppMessage> messages = messageRepository.findAll();

        // Filter out messages where the sender is the same as PHONE_NUMBER
        return messages.stream()
                .filter(message -> !message.getSender().equals(PHONE_NUMBER)) // Exclude own messages
                .collect(Collectors.groupingBy(WhatsAppMessage::getSender)) // Group by sender
                .entrySet()
                .stream()
                .map(entry -> {
                    String sender = entry.getKey();
                    List<WhatsAppMessage> senderMessages = entry.getValue();

                    // Find the most recent message
                    WhatsAppMessage latestMessage = senderMessages.stream()
                            .max(Comparator.comparing(WhatsAppMessage::getSentDate))
                            .orElseThrow();

                    // Count unread messages for the sender
                    long unreadCount = senderMessages.stream()
                            .filter(WhatsAppMessage::isUnread)
                            .count();

                    // Map to WhatsAppConversation
                    return new WhatsAppConversation(sender, latestMessage.getSentDate(), (int) unreadCount);
                })
                .collect(Collectors.toList());
    }
    @Override
    public List<WhatsAppMessage> getMessages(String sender) {
        // Fetch all messages from the repository
        List<WhatsAppMessage> messages = messageRepository.findAll();

        // Filter messages that are either sent or received by the given sender and the current user
        return messages.stream()
                .filter(message ->
                        (message.getSender().equals(sender) && message.getRecipient().equals(PHONE_NUMBER)) ||
                                (message.getSender().equals(PHONE_NUMBER) && message.getRecipient().equals(sender))
                )
                .sorted(Comparator.comparing(WhatsAppMessage::getSentDate)) // Optional: Sort by timestamp
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasUserMessagedInLast24Hours(String sender) {
        Date twentyFourHoursAgo = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        List<WhatsAppMessage> messages = messageRepository.findBySenderAndSentDateAfter(sender, twentyFourHoursAgo);
        return !messages.isEmpty();
    }

    @Override
    @Transactional
    public void markConversationAsRead(String sender) {
        List<WhatsAppMessage> unreadMessages = messageRepository.findBySenderAndUnreadTrue(sender);
        unreadMessages.forEach(message -> message.setUnread(false));
        messageRepository.saveAll(unreadMessages);
    }

    @Override
    public long getUnreadMessageCountBySender(String sender) {
        return messageRepository.countUnreadMessagesBySender(sender);
    }

    @Override
    public List<WhatsAppMessage> getAllUnreadMessages() {
        return messageRepository.findByUnreadTrue();
    }
}
