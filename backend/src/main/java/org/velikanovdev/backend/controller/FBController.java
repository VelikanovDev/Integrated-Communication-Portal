package org.velikanovdev.backend.controller;

import com.restfb.*;
import com.restfb.json.JsonObject;
import com.restfb.types.Conversation;
import com.restfb.types.Message;
import com.restfb.types.send.SendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FBController {
    @Value("${facebook.access.token}")
    private String ACCESS_TOKEN;

    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations() {
        FacebookClient facebookClient = new DefaultFacebookClient(ACCESS_TOKEN, Version.LATEST);

        // Fetch conversations using RestFB
        Connection<Conversation> conversationList = facebookClient.fetchConnection(
                "me/conversations",
                Conversation.class,
                Parameter.with("fields", "id,participants,updated_time")
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
