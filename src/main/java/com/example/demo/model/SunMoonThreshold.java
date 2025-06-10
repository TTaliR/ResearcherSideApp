package com.example.demo.model;

import java.util.List;

public class SunMoonThreshold {

    private List<AzimuthRange> sunAzimuthRanges; // List of ranges for the Sun
    private List<AzimuthRange> moonAzimuthRanges; // List of ranges for the Moon

    // Getters and setters
    public List<AzimuthRange> getSunAzimuthRanges() {
        return sunAzimuthRanges;
    }

    public void setSunAzimuthRanges(List<AzimuthRange> sunAzimuthRanges) {
        this.sunAzimuthRanges = sunAzimuthRanges;
    }
    
    

   public List<AzimuthRange> getMoonAzimuthRanges() {
        return moonAzimuthRanges;
    }

    public void setMoonAzimuthRanges(List<AzimuthRange> moonAzimuthRanges) {
        this.moonAzimuthRanges = moonAzimuthRanges;
    }

}
