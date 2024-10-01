package org.velikanovdev.backend.service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.stereotype.Service;
import org.velikanovdev.backend.entity.ReceivedEmail;

import java.io.IOException;
import java.util.*;

@Service
public class EmailReceiverService {

    public List<List<ReceivedEmail>> fetchEmailConversations(String host, String storeType, String user, String password) {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", storeType);
            properties.put("mail." + storeType + ".host", host);
            properties.put("mail." + storeType + ".port", "993");
            properties.put("mail." + storeType + ".starttls.enable", "true");

            Session emailSession = Session.getDefaultInstance(properties);

            Store store = emailSession.getStore(storeType);
            store.connect(host, user, password);

            List<ReceivedEmail> allReceivedEmails = new ArrayList<>();
            // Fetch from Inbox
            fetchMessagesFromFolder(store, "INBOX", allReceivedEmails);

            // Fetch from Sent Items
            fetchMessagesFromFolder(store, "[Gmail]/Sent Mail", allReceivedEmails); // Gmail specific, adjust for other providers

            store.close();

            // Group emails by thread
            return groupEmailsByConversation(allReceivedEmails);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void fetchMessagesFromFolder(Store store, String folderName, List<ReceivedEmail> receivedEmails) throws MessagingException, IOException {
        Folder folder = store.getFolder(folderName);
        folder.open(Folder.READ_ONLY);

        Message[] messages = folder.getMessages();
        for (Message message : messages) {
            String[] references = message.getHeader("References");
            String referencesHeader = (references != null && references.length > 0) ? String.join(" ", references) : null;

            String from = message.getFrom()[0].toString();
            receivedEmails.add(new ReceivedEmail(
                    from.substring(from.indexOf('<') + 1, from.indexOf('>')),
                    message.getSubject(),
                    getTextFromMessage(message),
                    message.getHeader("Message-ID")[0],
                    message.getHeader("In-Reply-To") != null ? message.getHeader("In-Reply-To")[0] : null,
                    referencesHeader,
                    message.getSentDate(),
                    message.getAllRecipients()[0].toString()
            ));
        }

        folder.close(false);
    }

    private List<List<ReceivedEmail>> groupEmailsByConversation(List<ReceivedEmail> receivedEmails) {
        Map<String, List<ReceivedEmail>> conversationMap = new HashMap<>();
        Map<String, String> messageIdToThreadKey = new HashMap<>();

        for (ReceivedEmail email : receivedEmails) {
            // Determine thread key for this email based on References or In-Reply-To
            String threadKey = determineThreadKey(email, messageIdToThreadKey);

            // Associate the email's Message-ID with the thread key
            if (email.getMessageId() != null) {
                messageIdToThreadKey.put(email.getMessageId(), threadKey);
            }

            // Add the email to the correct conversation
            conversationMap.putIfAbsent(threadKey, new ArrayList<>());
            conversationMap.get(threadKey).add(email);
        }

        List<List<ReceivedEmail>> conversations = new ArrayList<>(conversationMap.values());

        // Sort each conversation by date
        for (List<ReceivedEmail> conversation : conversations) {
            conversation.sort(Comparator.comparing(ReceivedEmail::getSentDate));
        }

        return conversations;
    }

    private String determineThreadKey(ReceivedEmail email, Map<String, String> messageIdToThreadKey) {
        // Check the References header first, as it gives the full chain of message IDs
        if (email.getReferences() != null && !email.getReferences().isEmpty()) {
            String[] references = email.getReferences().split("\\s+");
            for (String reference : references) {
                if (messageIdToThreadKey.containsKey(reference)) {
                    return messageIdToThreadKey.get(reference);  // Return the existing thread key
                }
            }
        }

        // If no match in References, check the In-Reply-To header
        if (email.getInReplyTo() != null && messageIdToThreadKey.containsKey(email.getInReplyTo())) {
            return messageIdToThreadKey.get(email.getInReplyTo());
        }

        // If no match in either, use the Message-ID as the thread key
        return email.getMessageId();
    }


    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break, the same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
            }
        }
        return result;
    }
}
