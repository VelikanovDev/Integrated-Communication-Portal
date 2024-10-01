package org.velikanovdev.backend.controller;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.velikanovdev.backend.entity.ReceivedEmail;
import org.velikanovdev.backend.entity.ReplyEmail;
import org.velikanovdev.backend.service.EmailReceiverService;
import org.velikanovdev.backend.service.EmailSenderService;

import java.util.List;

@RestController
public class EmailController {
    private final EmailReceiverService emailReceiverService;
    private final EmailSenderService emailSenderService;

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

    @GetMapping("/fetch-emails")
    public ResponseEntity<List<List<ReceivedEmail>>> fetchEmailConversations() {
        List<List<ReceivedEmail>> conversations = emailReceiverService.fetchEmailConversations(host, storeType, user, password);
        return ResponseEntity.ok(conversations);
    }


    @PostMapping("/reply-email")
    public String replyEmail(@RequestBody ReplyEmail replyEmail) throws MessagingException {
        emailSenderService.replyToEmail(replyEmail.getRecipient(), replyEmail.getSubject(), replyEmail.getMessage(), replyEmail.getMessageId());
        return "Email replied successfully!";
    }
}
