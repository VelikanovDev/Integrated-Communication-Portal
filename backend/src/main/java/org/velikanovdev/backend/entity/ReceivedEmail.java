package org.velikanovdev.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedEmail {
    private String from;
    private String subject;
    private String body;
    private String messageId;
    private String inReplyTo;
    private String references;
    private Date sentDate;
    private String to;
}
