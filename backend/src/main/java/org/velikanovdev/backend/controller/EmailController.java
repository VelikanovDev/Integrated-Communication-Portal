package org.velikanovdev.backend.controller;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.velikanovdev.backend.entity.EmailConversation;
import org.velikanovdev.backend.entity.ReplyEmail;
import org.velikanovdev.backend.service.EmailReceiverService;
import org.velikanovdev.backend.service.EmailSenderService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
public class EmailController {
    private final EmailReceiverService emailReceiverService;
    private final EmailSenderService emailSenderService;
    private final List<SseEmitter> emitters = Collections.synchronizedList(new ArrayList<>());

    @Value("${email.receiver.host}")
    private String host;

    @Value("${email.receiver.storeType}")
    private String storeType;

    @Value("${email.receiver.username}")
    private String user;

    @Value("${email.receiver.password}")
    private String password;

    @Autowired
    public EmailController(EmailReceiverService emailReceiverService, EmailSenderService emailSenderService) {
        this.emailReceiverService = emailReceiverService;
        this.emailSenderService = emailSenderService;
    }

    @GetMapping("/email/notifications")
    public SseEmitter getEmailNotifications() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 minutes timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    // Notify all clients with the latest email conversations
    private void notifyClients(List<EmailConversation> conversations) {
        List<SseEmitter> emittersToRemove = new ArrayList<>();

        // Iterate over a snapshot of the emitters to avoid ConcurrentModificationException
        for (SseEmitter emitter : new ArrayList<>(emitters)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("emailConversations")
                        .data(conversations));
            } catch (IOException e) {
                emittersToRemove.add(emitter);
            }
        }

        // Remove emitters that are no longer valid
        emitters.removeAll(emittersToRemove);
    }

    @Scheduled(fixedRate = 10000)
    public void pollEmailsAndNotify() {
        try {
            List<EmailConversation> emailConversations =
                    emailReceiverService.fetchEmailConversations(host, storeType, user, password);

            notifyClients(emailConversations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/fetch-emails")
    public ResponseEntity<List<EmailConversation>> fetchEmailConversations() {
        List<EmailConversation> conversations = emailReceiverService.fetchEmailConversations(host, storeType, user, password);
        return ResponseEntity.ok(conversations);
    }


    @PostMapping("/reply-email")
    public String replyEmail(@RequestBody ReplyEmail replyEmail) throws MessagingException {
        emailSenderService.replyToEmail(replyEmail.getRecipient(), replyEmail.getSubject(), replyEmail.getMessage(), replyEmail.getMessageId());
        return "Email replied successfully!";
    }

    @PutMapping("/mark-as-read/{conversationId}")
    public String markConversationAsRead(@PathVariable String conversationId) {
        List<EmailConversation> emailConversations = fetchEmailConversations().getBody();
        EmailConversation conversation = emailConversations.stream()
                .filter(c -> c.getConversationId().equals(conversationId))
                .findFirst()
                .orElse(null);

        if (conversation == null) {
            return "Conversation not found!";
        }
        emailSenderService.markConversationAsRead(host, storeType, user, password, conversation);
        return "Email marked as read!";
    }
}
