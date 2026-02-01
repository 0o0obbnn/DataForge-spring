package com.dataforge.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "generation_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerationHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "template_id")
  private Long templateId;

  @Column(name = "template_name")
  private String templateName;

  @Column(name = "record_count", nullable = false)
  private Integer recordCount;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
