package com.example.demo.model;
import java.util.List;

public class HeartRateRange {

    private List<HeartRateThresholdMapping> thresholds;

    public List<HeartRateThresholdMapping> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<HeartRateThresholdMapping> thresholds) {
        this.thresholds = thresholds;
    }

    /** 
     *  This class represents a single heart rate range with a vibration pattern.
     */
    public static class HeartRateThresholdMapping {
    	private int id;
        private int min;          // Minimum heart rate
        private int max;          // Maximum heart rate
        private int pulses;       // Number of pulses per pattern
        private int intensity;    // Vibration intensity (1 = Low, 2 = Medium, 3 = High)
        private int duration;     // Duration of each pulse in milliseconds
        private int interval;     // Time between pulses in milliseconds
        private boolean active = true;

        public HeartRateThresholdMapping() {}

        public HeartRateThresholdMapping(int min, int max, int intensity, int pulses, int duration, int interval) {
        	this.min = min;
            this.max = max;
            this.intensity = intensity;
            this.pulses = pulses;
            this.duration = duration;
            this.interval = interval;
        }

        public int getMin() {
            return min;
        }

        public void setMin(int min) {
            this.min = min;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public int getIntensity() {
            return intensity;
        }

        public void setIntensity(int intensity) {
            this.intensity = intensity;
        }

        public int getPulses() {
            return pulses;
        }

        public void setPulses(int pulses) {
            this.pulses = pulses;
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


//
//		@Override
//		public String toString() {
//			return "HeartRateThresholdMapping [id=" + id + ", min=" + min + ", max=" + max + ", pulses=" + pulses
//					+ ", intensity=" + intensity + ", duration=" + duration + ", interval=" + interval + ", active="
//					+ active + "]";
//		}

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
}
