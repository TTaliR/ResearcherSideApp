package com.example.demo.model;

import java.time.LocalDateTime;

public class Alert {
    private Long id;
    private String alertType;
    private LocalDateTime timestamp;
    private FeedbackConfig feedbackConfig;

    // Constructors (optional)
    public Alert() {}

    public Alert(Long id, String alertType, LocalDateTime timestamp, FeedbackConfig feedbackConfig) {
        this.id = id;
        this.alertType = alertType;
        this.timestamp = timestamp;
        this.feedbackConfig = feedbackConfig;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public FeedbackConfig getFeedbackConfig() {
        return feedbackConfig;
    }

    public void setFeedbackConfig(FeedbackConfig feedbackConfig) {
        this.feedbackConfig = feedbackConfig;
    }
}
