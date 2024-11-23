package org.velikanovdev.backend.controller;

import com.restfb.*;
import com.restfb.json.JsonObject;
import com.restfb.types.Conversation;
import com.restfb.types.Message;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.send.SendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.velikanovdev.backend.entity.ConversationDetail;
import org.velikanovdev.backend.entity.MessageDetail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class FBController {
    @Value("${facebook.access.token}")
    private String ACCESS_TOKEN;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private long lastUnreadCount = 0;

    // SSE endpoint for frontend to listen for unread message notifications
    @GetMapping("/facebook/notifications")
    public SseEmitter getUnreadMessageNotifications() {
        SseEmitter emitter = new SseEmitter();
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    @Scheduled(fixedRate = 10000)
    public void pollConversations() {
        FacebookClient facebookClient = new DefaultFacebookClient(ACCESS_TOKEN, Version.LATEST);

        // Fetch all conversations with relevant fields
        Connection<Conversation> conversationList = facebookClient.fetchConnection(
                "me/conversations",
                Conversation.class,
                Parameter.with("fields", "id,participants,updated_time,messages{message,from,to,created_time},unread_count")
        );

        List<ConversationDetail> conversationDetails = conversationList.getData().stream()
                .map(conversation -> {
                    // Extract primary participant
                    NamedFacebookType primaryParticipant = conversation.getParticipants().stream()
                            .findFirst()
                            .orElse(null);

                    String participantName = primaryParticipant != null ? primaryParticipant.getName() : "Unknown";
                    String participantId = primaryParticipant != null ? primaryParticipant.getId() : null;

                    // Fetch all messages (handle pagination)
                    List<Message> allMessages = fetchAllMessages(facebookClient, conversation.getId());

                    // Count messages from the primary participant
                    long messagesFromPrimaryParticipant = allMessages.stream()
                            .filter(message -> message.getFrom() != null && message.getFrom().getId().equals(participantId))
                            .count();

                    // Map messages to a simplified structure
                    List<MessageDetail> messageDetails = allMessages.stream()
                            .map(message -> new MessageDetail(
                                    message.getId(),
                                    message.getMessage(),
                                    message.getFrom() != null ? message.getFrom().getName() : "Unknown",
                                    message.getTo().stream().map(NamedFacebookType::getName).toList(),
                                    message.getCreatedTime()
                            ))
                            .toList();

                    // Create a detailed conversation response object
                    return new ConversationDetail(
                            conversation.getId(),
                            participantName,
                            conversation.getUpdatedTime(),
                            messagesFromPrimaryParticipant,
                            messageDetails
                    );
                })
                .toList();

        // Notify clients with the updated list of conversations
        notifyClients(conversationDetails);
    }

    private List<Message> fetchAllMessages(FacebookClient facebookClient, String conversationId) {
        List<Message> allMessages = new ArrayList<>();

        // Fetch the first page of messages
        Connection<Message> messages = facebookClient.fetchConnection(
                conversationId + "/messages",
                Message.class,
                Parameter.with("fields", "id,message,from,to,created_time")
        );

        // Add messages from the first page
        allMessages.addAll(messages.getData());

        // Fetch subsequent pages
        while (messages.hasNext()) {
            messages = facebookClient.fetchConnectionPage(messages.getNextPageUrl(), Message.class);
            allMessages.addAll(messages.getData());
        }

        return allMessages;
    }

    private void notifyClients(List<ConversationDetail> conversations) {
        List<SseEmitter> emittersToRemove = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("facebookConversations")
                        .data(conversations));
            } catch (IOException e) {
                // Add to the list of emitters to remove
                emittersToRemove.add(emitter);
            }
        }

        // Remove disconnected emitters
        emitters.removeAll(emittersToRemove);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations() {
        FacebookClient facebookClient = new DefaultFacebookClient(ACCESS_TOKEN, Version.LATEST);

        // Fetch conversations using RestFB
        Connection<Conversation> conversationList = facebookClient.fetchConnection(
                "me/conversations",
                Conversation.class,
                Parameter.with("fields", "id,participants,updated_time, messages{message,from,to,created_time}")
        );


        return ResponseEntity.ok(conversationList.getData());
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<Message>> getConversation(@PathVariable String conversationId) {
        FacebookClient facebookClient = new DefaultFacebookClient(ACCESS_TOKEN, Version.LATEST);
        Connection<Message> messages = facebookClient.fetchConnection(conversationId + "/messages", Message.class,
                Parameter.with("fields", "id,message,from,to,createdTime"));

        return ResponseEntity.ok(messages.getData());
    }

    @GetMapping("/message/{messageId}")
    public ResponseEntity<Message> getMessage(@PathVariable String messageId) {
        FacebookClient facebookClient = new DefaultFacebookClient(ACCESS_TOKEN, Version.LATEST);
        Message message = facebookClient.fetchObject(messageId, Message.class, Parameter.with("fields", "id,message,from,to"));

        return ResponseEntity.ok(message);
    }

    @PostMapping("/sendMessage/{recipientId}")
    public ResponseEntity<String> sendMessage(@PathVariable String recipientId, @RequestParam String messageText) {
        FacebookClient fbClient = new DefaultFacebookClient(ACCESS_TOKEN, Version.LATEST);

        // Create recipient
        JsonObject recipient = new JsonObject();
        recipient.add("id", recipientId);

        // Create message
        JsonObject message = new JsonObject();
        message.add("text", messageText);

        // Create payload
        JsonObject payload = new JsonObject();
        payload.add("recipient", recipient);
        payload.add("message", message);

        System.out.println("Payload: " + payload);

        // Send message
        try {
            SendResponse response = fbClient.publish("me/messages", SendResponse.class,
                    Parameter.with("recipient", recipient),
                    Parameter.with("message", message));
            return ResponseEntity.ok("Message sent successfully: Recipient ID - " + response.getRecipientId() +
                    ", Message ID - " + response.getMessageId());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending message: " + e.getMessage());
        }
    }
}
