package com.project.booktour.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class History {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "action_type", length = 255, nullable = false)
    private String actionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}