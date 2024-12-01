package org.velikanovdev.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailConversation {
    private String conversationId;
    private String sender;
    private List<ReceivedEmail> emails;
    private long unreadCount;
    private Date lastEmailDate;
}
