package com.example.demo.model;

public class AzimuthRange {
    private int id;
	private int minAzimuth; // Minimum azimuth value
    private int maxAzimuth; // Maximum azimuth value
    private int pulses;        // Number of pulses
    private int intensity;     // Feedback intensity (1 = Low, 5 = High)
    private int duration;      // Duration of each pulse (milliseconds)
    private int interval;      // Interval between pulses (milliseconds)
    private boolean active = true;

    // Default constructor
    public AzimuthRange() {}

    // Parameterized constructor
    public AzimuthRange(int minAzimuth, int maxAzimuth, int pulses, int intensity, int duration, int interval) {
        this.minAzimuth = minAzimuth;
        this.maxAzimuth = maxAzimuth;
        this.pulses = pulses;
        this.intensity = intensity;
        this.duration = duration;
        this.interval = interval;
    }

    // Getters and setters
    public int getMinAzimuth() {
        return minAzimuth;
    }

    public void setMinAzimuth(int minAzimuth) {
        this.minAzimuth = minAzimuth;
    }

    public int getMaxAzimuth() {
        return maxAzimuth;
    }

    public void setMaxAzimuth(int maxAzimuth) {
        this.maxAzimuth = maxAzimuth;
    }

    public int getPulses() {
        return pulses;
    }

    public void setPulses(int pulses) {
        this.pulses = pulses;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
