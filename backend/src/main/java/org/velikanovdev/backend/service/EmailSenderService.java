package org.velikanovdev.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {
    private final JavaMailSender mailSender;
    @Value("${omnichannel.email}")
    private String myEmail;

    @Autowired
    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(myEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void replyToEmail(String toEmail, String subject, String body, String messageId) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        helper.setFrom(myEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, false);

        // Set the threading headers to maintain the conversation
        if (messageId != null) {
            mimeMessage.setHeader("In-Reply-To", messageId);
            mimeMessage.setHeader("References", messageId);
        }

        mailSender.send(mimeMessage);
    }
}
