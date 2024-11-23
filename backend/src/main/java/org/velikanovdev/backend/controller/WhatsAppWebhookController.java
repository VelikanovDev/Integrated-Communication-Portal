package org.velikanovdev.backend.controller;

import com.restfb.DefaultJsonMapper;
import com.restfb.JsonMapper;
import com.restfb.types.webhook.Change;
import com.restfb.types.webhook.WebhookEntry;
import com.restfb.types.webhook.WebhookObject;
import com.restfb.types.webhook.whatsapp.WhatsappMessagesValue;
import com.restfb.types.whatsapp.platform.message.Text;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.velikanovdev.backend.entity.WhatsAppMessage;
import org.velikanovdev.backend.service.WhatsAppMessageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
public class WhatsAppWebhookController {
    private final WhatsAppMessageService messageService;
    private final List<SseEmitter> whatsappEmitters = Collections.synchronizedList(new ArrayList<>());

    @Value("${whatsapp.phone.number}")
    private String PHONE_NUMBER;
    @Value("${whatsapp.verify.token}")
    private String VERIFY_TOKEN;

    @Autowired
    public WhatsAppWebhookController(WhatsAppMessageService messageService) {
        this.messageService = messageService;
    }



    @GetMapping("/webhook")
    public String verifyWebhook(@RequestParam("hub.mode") String mode,
                                @RequestParam("hub.challenge") String challenge,
                                @RequestParam("hub.verify_token") String verifyToken) {
        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(verifyToken)) {
            System.out.println("Webhook verified");
            return challenge;
        } else {
            System.out.println("Verification failed");
            return "Verification failed";
        }
    }

    @PostMapping("/webhook")
    public void receiveWebhook(@RequestBody String payload) {
        JsonMapper jsonMapper = new DefaultJsonMapper();
        WebhookObject webhookObject = jsonMapper.toJavaObject(payload, WebhookObject.class);

        List<WhatsAppMessage> newMessages = new ArrayList<>();

        for (WebhookEntry entry : webhookObject.getEntryList()) {
            for (Change change : entry.getChanges()) {
                WhatsappMessagesValue value = (WhatsappMessagesValue) change.getValue();
                if (value != null) {
                    value.getMessages().forEach(message -> {
                        String sender = message.getFrom();
                        Text text = message.getText();
                        if (sender != null && text != null) {
                            System.out.println("Received message from " + sender + ": " + text.getBody());
                            WhatsAppMessage msg = new WhatsAppMessage();
                            msg.setSender(sender);
                            msg.setRecipient(PHONE_NUMBER);
                            msg.setMessage(text.getBody());
                            msg.setMessageId(message.getId());
                            msg.setSentDate(message.getTimestamp());
                            msg.setUnread(true);

                            messageService.saveMessage(msg);
                            newMessages.add(msg);
                        }
                    });
                }
            }
        }

        // Notify clients with new messages
        notifyWhatsAppClients(newMessages);
    }

    private void notifyWhatsAppClients(List<WhatsAppMessage> newMessages) {
        List<SseEmitter> emittersToRemove = new ArrayList<>();

        for (SseEmitter emitter : whatsappEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("whatsappMessages")
                        .data(newMessages));
            } catch (IOException e) {
                emittersToRemove.add(emitter);
            }
        }

        // Remove disconnected emitters
        whatsappEmitters.removeAll(emittersToRemove);
    }
}
