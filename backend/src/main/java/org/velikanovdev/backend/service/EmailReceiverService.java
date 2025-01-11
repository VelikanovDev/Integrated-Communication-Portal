package org.velikanovdev.backend.service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.stereotype.Service;
import org.velikanovdev.backend.entity.EmailConversation;
import org.velikanovdev.backend.entity.ReceivedEmail;

import java.io.IOException;
import java.util.*;

@Service
public class EmailReceiverService {

    public List<EmailConversation> fetchEmailConversations(String host, String storeType, String user, String password) {
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

            // Group emails into conversations
            return groupEmailsByConversation(allReceivedEmails);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private void fetchMessagesFromFolder(Store store, String folderName, List<ReceivedEmail> receivedEmails) throws MessagingException, IOException {
        Folder folder = store.getFolder(folderName);
        folder.open(Folder.READ_ONLY);

        Message[] messages = folder.getMessages();
        for (Message message : messages) {
            String[] references = message.getHeader("References");
            String referencesHeader = (references != null && references.length > 0) ? String.join(" ", references) : null;

            // Check if the email is read
            boolean isRead = message.isSet(Flags.Flag.SEEN);

            String fromEmail = getEmailAddress(message.getFrom()[0].toString());
            String toEmail = getEmailAddress(message.getAllRecipients()[0].toString());

            receivedEmails.add(new ReceivedEmail(
                    fromEmail,
                    message.getSubject(),
                    getTextFromMessage(message),
                    message.getHeader("Message-ID")[0],
                    message.getHeader("In-Reply-To") != null ? message.getHeader("In-Reply-To")[0] : null,
                    referencesHeader,
                    message.getSentDate(),
                    toEmail,
                    isRead
            ));
        }

        folder.close(false);
    }

    private String getEmailAddress(String fullMessageAddress) {
        String emailAddress;
        int start = fullMessageAddress.indexOf('<');
        int end = fullMessageAddress.indexOf('>');
        if (start != -1 && end != -1 && start < end) {
            emailAddress = fullMessageAddress.substring(start + 1, end);
        } else {
            emailAddress = fullMessageAddress; // Fallback to the full string if no angle brackets are found
        }

        return emailAddress;
    }

    private List<EmailConversation> groupEmailsByConversation(List<ReceivedEmail> receivedEmails) {
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

        // Convert the grouped emails into EmailConversation objects
        List<EmailConversation> conversations = new ArrayList<>();
        for (Map.Entry<String, List<ReceivedEmail>> entry : conversationMap.entrySet()) {
            String conversationId = entry.getKey();
            List<ReceivedEmail> emails = entry.getValue();
            long unreadMessages = emails.stream().filter(email -> !email.isRead()).count();

            String sender = emails.get(0).getFrom();

            // Find the most recent email date
            Date lastEmailDate = emails.stream()
                    .map(ReceivedEmail::getSentDate)
                    .max(Date::compareTo)
                    .orElse(null);

            conversations.add(new EmailConversation(conversationId, sender, emails, unreadMessages, lastEmailDate));
        }

        // Sort each conversation by date
        conversations.forEach(conversation ->
                conversation.getEmails().sort(Comparator.comparing(ReceivedEmail::getSentDate))
        );

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
        String content = "";
        if (message.isMimeType("text/plain")) {
            content = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            content = getTextFromMimeMultipart(mimeMultipart);
        }
        return cleanEmailBody(content);
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
                break; // Stop after getting the plain text content
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result.append(org.jsoup.Jsoup.parse(html).text());
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return cleanEmailBody(result.toString());
    }

    private String cleanEmailBody(String emailBody) {
        if (emailBody == null || emailBody.isEmpty()) {
            return emailBody;
        }

        // Split the email body into lines
        String[] lines = emailBody.split("\\r?\\n");
        StringBuilder cleanedBody = new StringBuilder();

        for (String line : lines) {
            // Skip lines that start with ">" (quoted text) or "On <date>, <email> wrote:"
            if (line.startsWith(">") || line.matches("^On .* wrote:$")) {
                break; // Stop processing further as this is the beginning of quoted text
            }
            cleanedBody.append(line).append("\n");
        }

        return cleanedBody.toString().trim();
    }
}
