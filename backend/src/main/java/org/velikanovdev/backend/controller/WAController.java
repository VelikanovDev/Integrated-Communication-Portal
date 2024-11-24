package org.velikanovdev.backend.controller;

import com.restfb.DefaultFacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.velikanovdev.backend.dto.WhatsAppMessageDto;
import org.velikanovdev.backend.entity.WhatsAppConversation;
import org.velikanovdev.backend.entity.WhatsAppMessage;
import org.velikanovdev.backend.service.WhatsAppMessageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/whatsapp")
public class WAController {
    private final WhatsAppMessageService messageService;
    private final List<SseEmitter> emitters = Collections.synchronizedList(new ArrayList<>());

    @Value("${facebook.access.token}")
    private String ACCESS_TOKEN;

    @Value("${whatsapp.phone.number.id}")
    private String PHONE_NUMBER_ID;

    @Value("${whatsapp.phone.number}")
    private String PHONE_NUMBER;

    @Autowired
    public WAController(WhatsAppMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/notifications")
    public SseEmitter getWhatsAppNotifications() {
        SseEmitter emitter = new SseEmitter();
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    // Scheduled task to poll WhatsApp messages
    @Scheduled(fixedRate = 10000)
    public void pollWhatsAppConversations() {
        List<WhatsAppConversation> conversationList = messageService.getConversations();

        List<WhatsAppConversation> conversationDetails = conversationList.stream()
                .map(conversation -> {
                    // Retrieve unread count from unread field in the messages
                    long unreadCount = messageService.getUnreadMessageCountBySender(conversation.getSender());

                    // Create a detailed conversation response object
                    return new WhatsAppConversation(
                            conversation.getSender(),
                            conversation.getLastMessageDate(),
                            (int) unreadCount
                    );
                })
                .toList();

        // Notify clients
        notifyWhatsAppClients(conversationDetails);
    }

    private void notifyWhatsAppClients(List<WhatsAppConversation> conversations) {
        List<SseEmitter> emittersToRemove = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("whatsappConversations")
                        .data(conversations));
            } catch (IOException e) {
                emittersToRemove.add(emitter);
            }
        }

        emitters.removeAll(emittersToRemove);
    }

    @PostMapping("/sendMessage/{recipientPhone}")
    public ResponseEntity<String> sendMessage(@PathVariable String recipientPhone, @RequestBody WhatsAppMessageDto message) {
        DefaultFacebookClient fbClient = new DefaultFacebookClient(ACCESS_TOKEN, Version.LATEST);
        fbClient.setHeaderAuthorization(true);

        // Check if the user has messaged in the last 24 hours
        if (!hasUserMessagedInLast24Hours(recipientPhone)) {
            return ResponseEntity.badRequest().body("User has not messaged in the last 24 hours");
        }

        JsonObject textJso = new JsonObject();
        textJso.add("preview_url", false);
        textJso.add("body", message.getMessageText());

        WhatsAppMessage msg = new WhatsAppMessage();
        msg.setSender(PHONE_NUMBER);
        msg.setRecipient(recipientPhone);
        msg.setMessage(message.getMessageText());
        msg.setSentDate(new Date());
        msg.setUnread(false);

        Parameter[] params = new Parameter[]{
                Parameter.with("messaging_product", "whatsapp"),
                Parameter.with("recipient_type", "individual"),
                Parameter.with("type", "text"),
                Parameter.with("to", recipientPhone),
                Parameter.with("text", textJso)};

        // Send the data and get the result as JsonObject
        JsonObject jso = fbClient.publish(PHONE_NUMBER_ID + "/messages", JsonObject.class, params);

        messageService.saveMessage(msg);
        return ResponseEntity.ok("Message sent");
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<WhatsAppConversation>> getConversations() {
        List<WhatsAppConversation> conversations = messageService.getConversations();
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/messages/{sender}")
    public ResponseEntity<List<WhatsAppMessage>> getMessages(@PathVariable String sender) {
        List<WhatsAppMessage> messages = messageService.getMessages(sender);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/mark-as-read/{sender}")
    public ResponseEntity<Void> markAsRead(@PathVariable String sender) {
        try {
            messageService.markConversationAsRead(sender);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/unread-messages")
    public ResponseEntity<List<WhatsAppMessage>> getUnreadMessages() {
        try {
            List<WhatsAppMessage> unreadMessages = messageService.getAllUnreadMessages();
            return ResponseEntity.ok(unreadMessages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean hasUserMessagedInLast24Hours(String sender) {
        return messageService.hasUserMessagedInLast24Hours(sender);
    }
}
