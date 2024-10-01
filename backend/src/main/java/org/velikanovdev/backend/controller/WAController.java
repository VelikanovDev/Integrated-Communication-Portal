package org.velikanovdev.backend.controller;

import com.restfb.DefaultFacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.velikanovdev.backend.dto.WhatsAppMessageDto;
import org.velikanovdev.backend.entity.WhatsAppConversation;
import org.velikanovdev.backend.entity.WhatsAppMessage;
import org.velikanovdev.backend.service.WhatsAppMessageService;

import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/whatsapp")
public class WAController {
    private final WhatsAppMessageService messageService;

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

    private boolean hasUserMessagedInLast24Hours(String sender) {
        return messageService.hasUserMessagedInLast24Hours(sender);
    }
}
