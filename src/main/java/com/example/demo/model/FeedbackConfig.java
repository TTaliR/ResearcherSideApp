package com.example.demo.model;

import java.util.List;

public class FeedbackConfig {

    private Long id;

    private Integer pulses;
    private Integer intensity;
    private Integer duration;
    private Integer interval;

    private Integer minvalue;
    private Integer maxvalue;

    private String type;

    private List<Alert> alerts;

    // Constructors (optional)
    public FeedbackConfig() {}

    public FeedbackConfig(Long id, Integer pulses, Integer intensity, Integer duration,
                          Integer interval, Integer minvalue, Integer maxvalue,
                          String type, List<Alert> alerts) {
        this.id = id;
        this.pulses = pulses;
        this.intensity = intensity;
        this.duration = duration;
        this.interval = interval;
        this.minvalue = minvalue;
        this.maxvalue = maxvalue;
        this.type = type;
        this.alerts = alerts;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPulses() {
        return pulses;
    }

    public void setPulses(Integer pulses) {
        this.pulses = pulses;
    }

    public Integer getIntensity() {
        return intensity;
    }

    public void setIntensity(Integer intensity) {
        this.intensity = intensity;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getMinvalue() {
        return minvalue;
    }

    public void setMinvalue(Integer minvalue) {
        this.minvalue = minvalue;
    }

    public Integer getMaxvalue() {
        return maxvalue;
    }

    public void setMaxvalue(Integer maxvalue) {
        this.maxvalue = maxvalue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }
}
