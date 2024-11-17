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
public class MessageDetail {
    private String id;
    private String message;
    private String from;
    private List<String> to;
    private Date createdTime;
}
