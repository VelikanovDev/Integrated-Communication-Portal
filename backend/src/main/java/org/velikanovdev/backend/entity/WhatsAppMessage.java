package org.velikanovdev.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "msgs")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class WhatsAppMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;
    @Column
    private String sender;
    @Column
    private String recipient;
    @Column
    private String message;
    @Column
    private String messageId;
    @Column
    private Date sentDate;
    @Column
    private boolean unread = true;
}
