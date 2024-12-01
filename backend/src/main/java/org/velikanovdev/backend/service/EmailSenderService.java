package org.velikanovdev.backend.service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.velikanovdev.backend.entity.EmailConversation;
import org.velikanovdev.backend.entity.ReceivedEmail;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@Service
public class EmailSenderService {
    private final JavaMailSender mailSender;
    @Value("${omnichannel.email}")
    private String myEmail;

    @Autowired
    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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

    public void markConversationAsRead(String host, String storeType, String user, String password, EmailConversation conversation) {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", storeType);
            properties.put("mail." + storeType + ".host", host);
            properties.put("mail." + storeType + ".port", "993");
            properties.put("mail." + storeType + ".starttls.enable", "true");

            Session emailSession = Session.getDefaultInstance(properties);
            Store store = emailSession.getStore(storeType);
            store.connect(host, user, password);

            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.getMessages();
            Set<String> messageIdsToMark = new HashSet<>(); // Collect all message IDs to mark as read

            // Collect the Message-IDs from the conversation
            for (ReceivedEmail email : conversation.getEmails()) {
                messageIdsToMark.add(email.getMessageId());
            }

            // Mark each message in the conversation as read
            for (Message message : messages) {
                String[] messageIdHeader = message.getHeader("Message-ID");
                if (messageIdHeader != null && messageIdsToMark.contains(messageIdHeader[0])) {
                    message.setFlag(Flags.Flag.SEEN, true);
                }
            }

            folder.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
