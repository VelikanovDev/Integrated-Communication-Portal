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
public class ConversationDetail {
    private String id;
    private String participantName;
    private Date updatedTime;
    private Long messagesFromPrimaryParticipant;
    private List<FBMessageDetail> messages;
}
