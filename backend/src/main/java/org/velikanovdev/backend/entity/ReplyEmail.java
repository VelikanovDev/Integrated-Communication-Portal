package org.velikanovdev.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReplyEmail {
    private String recipient;
    private String subject;
    private String message;
    private String messageId;
}
