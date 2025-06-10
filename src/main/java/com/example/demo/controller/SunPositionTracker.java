package com.example.demo.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.example.demo.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.scene.control.Tooltip;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SunPositionTracker {

    private Stage stage;
    private int selectedUserID;
    private ScheduledExecutorService scheduler;
    private LineChart<Number, Number> sunChart;
    private XYChart.Series<Number, Number> sunTrajectory;
    private XYChart.Series<Number, Number> currentPosition;
    private XYChart.Series<Number, Number> feedbackPoints;
    private Circle sunCircle;
    private Label azimuthLabel;
    private Label elevationLabel;
    private Label timeLabel;
    private Label userFeedbackLabel;
    
    private List<SunPosition> sunPositionHistory = new ArrayList<>();
    private List<FeedbackEvent> feedbackEvents = new ArrayList<>();

    // Inner class to hold sun position data
    private static class SunPosition {
        private final double azimuth;
        private final double elevation;
        private final LocalDateTime timestamp;

        public SunPosition(double azimuth, double elevation, LocalDateTime timestamp) {
            this.azimuth = azimuth;
            this.elevation = elevation;
            this.timestamp = timestamp;
        }
    }
    
    // Inner class to hold feedback events
    private static class FeedbackEvent {
        private final double azimuth;
        private final double elevation;
        private final LocalDateTime timestamp;
        private final String type;
        private final int pulses;
        private final int intensity;

        public FeedbackEvent(double azimuth, double elevation, LocalDateTime timestamp, 
                            String type, int pulses, int intensity) {
            this.azimuth = azimuth;
            this.elevation = elevation;
            this.timestamp = timestamp;
            this.type = type;
            this.pulses = pulses;
            this.intensity = intensity;
        }
    }

    public SunPositionTracker(int userID) {
        this.selectedUserID = userID;
        initializeStage();
        startTracking();
    }

    private void initializeStage() {
        try {
            stage = new Stage();
            stage.setTitle("Live Sun Position Tracker - User " + selectedUserID);
            
            BorderPane root = new BorderPane();
            
            // Create chart for sun position
            NumberAxis xAxis = new NumberAxis(0, 360, 30);
            NumberAxis yAxis = new NumberAxis(0, 90, 10);
            xAxis.setLabel("Azimuth (degrees)");
            yAxis.setLabel("Elevation (degrees)");
            
            sunChart = new LineChart<>(xAxis, yAxis);
            sunChart.setTitle("Sun Position");
            sunChart.setAnimated(false);
            sunChart.setCreateSymbols(true);
            
            // Series for sun trajectory, current position and feedback points
            sunTrajectory = new XYChart.Series<>();
            sunTrajectory.setName("Sun Path Today");
            
            currentPosition = new XYChart.Series<>();
            currentPosition.setName("Current Position");
            
            feedbackPoints = new XYChart.Series<>();
            feedbackPoints.setName("Feedback Events");
            
            sunChart.getData().addAll(sunTrajectory, currentPosition, feedbackPoints);
            
            // Create information panel
            VBox infoPanel = new VBox(10);
            infoPanel.setStyle("-fx-padding: 15; -fx-background-color: #f4f4f4;");
            
            timeLabel = new Label("Time: --:--:--");
            timeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            azimuthLabel = new Label("Azimuth: -- degrees");
            azimuthLabel.setStyle("-fx-font-size: 14px;");
            
            elevationLabel = new Label("Elevation: -- degrees");
            elevationLabel.setStyle("-fx-font-size: 14px;");
            
            userFeedbackLabel = new Label("No recent feedback for User " + selectedUserID);
            userFeedbackLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: blue;");
            
            infoPanel.getChildren().addAll(
                timeLabel, 
                azimuthLabel, 
                elevationLabel,
                new Label(""), // spacer
                userFeedbackLabel
            );
            
            root.setCenter(sunChart);
            root.setRight(infoPanel);
            
            Scene scene = new Scene(root, 900, 600);
            stage.setScene(scene);
            
            // Handle stage closing
            stage.setOnCloseRequest(event -> {
                stopTracking();
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void show() {
        stage.show();
    }
    
    private void startTracking() {
        // Initialize scheduler for live updates
        scheduler = Executors.newScheduledThreadPool(1);
        
        // Schedule the task to run every 10 seconds
        scheduler.scheduleAtFixedRate(this::updateSunPosition, 0, 10, TimeUnit.SECONDS);
    }
    
    private void stopTracking() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
    
    private void updateSunPosition() {
        try {
            // Query the API for sun position and user feedback data
            URL url = new URL("http://localhost:1880/get-data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode dataNode = mapper.readTree(response.toString());
            
            // Process data to find latest sun position and feedback events
            processSunPositionData(dataNode);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void processSunPositionData(JsonNode dataNode) {
        if (!dataNode.isArray()) return;
        
        boolean foundCurrentPosition = false;
        LocalDateTime latestFeedbackTime = null;
        FeedbackEvent latestFeedback = null;
        
        for (JsonNode node : dataNode) {
            int sensorId = node.get("sensorid").asInt();
            
            // Only process sun position data (sensor ID 2)
            if (sensorId != 2) continue;
            
            String timeStr = node.get("time").asText();
            Instant instant = Instant.parse(timeStr);
            LocalDateTime dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            
            // Get azimuth value
            double azimuth = node.get("value").asDouble();
            
            // For simplicity, we'll use a calculated elevation based on time of day
            // In a real app, this would come from a sensor or calculation
            double elevation = calculateElevation(dateTime);
            
            // Store position in history
            sunPositionHistory.add(new SunPosition(azimuth, elevation, dateTime));
            
            // Keep only the last 24 hours of data in memory
            while (sunPositionHistory.size() > 144) { // 144 entries = 24h at 10min intervals
                sunPositionHistory.remove(0);
            }
            
            // Check if this is data for the selected user and has feedback
            int userId = node.get("userid").asInt();
            String alertType = node.hasNonNull("alert_type") ? node.get("alert_type").asText() : null;
            
            if (userId == selectedUserID && alertType != null) {
                int pulses = node.hasNonNull("pulses") ? node.get("pulses").asInt() : 0;
                int intensity = node.hasNonNull("intensity") ? node.get("intensity").asInt() : 0;
                
                FeedbackEvent event = new FeedbackEvent(
                    azimuth, elevation, dateTime, alertType, pulses, intensity
                );
                
                feedbackEvents.add(event);
                
                // Keep track of the latest feedback
                if (latestFeedbackTime == null || dateTime.isAfter(latestFeedbackTime)) {
                    latestFeedbackTime = dateTime;
                    latestFeedback = event;
                }
            }
            
            // If this is the most recent reading, update the current position
            if (!foundCurrentPosition || dateTime.isAfter(sunPositionHistory.get(sunPositionHistory.size() - 1).timestamp)) {
                foundCurrentPosition = true;
                
                // Update the UI on the JavaFX thread
                final LocalDateTime finalDateTime = dateTime;
                final double finalAzimuth = azimuth;
                final double finalElevation = elevation;
                final FeedbackEvent finalLatestFeedback = latestFeedback;
                
                Platform.runLater(() -> updateUI(finalDateTime, finalAzimuth, finalElevation, finalLatestFeedback));
            }
        }
    }
    
    private double calculateElevation(LocalDateTime time) {
        // Simple elevation model based on time of day
        // Peaks at noon, 0 at 6am/6pm, negative at night
        int hour = time.getHour();
        int minute = time.getMinute();
        
        double hourDecimal = hour + minute / 60.0;
        double dayProgress = (hourDecimal - 6) / 12.0; // 0 at 6am, 1 at 6pm
        
        if (dayProgress < 0) dayProgress = 0;
        if (dayProgress > 1) dayProgress = 0;
        
        // Simple sine wave to model sun elevation
        return Math.sin(dayProgress * Math.PI) * 90;
    }
    
    private void updateUI(LocalDateTime time, double azimuth, double elevation, FeedbackEvent latestFeedback) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        timeLabel.setText("Time: " + time.format(formatter));
        azimuthLabel.setText(String.format("Azimuth: %.2f degrees", azimuth));
        elevationLabel.setText(String.format("Elevation: %.2f degrees", elevation));
        
        // Update chart data
        updateChartData(azimuth, elevation, time);
        
        // Update latest feedback info
        if (latestFeedback != null) {
            LocalDateTime feedbackTime = latestFeedback.timestamp;
            // If feedback is from the last 5 minutes, highlight it
            if (feedbackTime.isAfter(time.minusMinutes(5))) {
                userFeedbackLabel.setText(String.format(
                    "Recent feedback at %s: %s (Pulses: %d, Intensity: %d)",
                    feedbackTime.format(formatter),
                    latestFeedback.type,
                    latestFeedback.pulses,
                    latestFeedback.intensity
                ));
                userFeedbackLabel.setTextFill(Color.RED);
            } else {
                userFeedbackLabel.setText(String.format(
                    "Last feedback at %s: %s",
                    feedbackTime.format(formatter),
                    latestFeedback.type
                ));
                userFeedbackLabel.setTextFill(Color.BLUE);
            }
        }
    }
    
    private void updateChartData(double azimuth, double elevation, LocalDateTime time) {
        // Clear and update the trajectory data
        sunTrajectory.getData().clear();
        
        // Add all points from today to the trajectory
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        
        for (SunPosition pos : sunPositionHistory) {
            if (pos.timestamp.isAfter(today)) {
                XYChart.Data<Number, Number> point = new XYChart.Data<>(pos.azimuth, pos.elevation);
                sunTrajectory.getData().add(point);
            }
        }
        
        // Update current position
        currentPosition.getData().clear();
        XYChart.Data<Number, Number> currentPoint = new XYChart.Data<>(azimuth, elevation);
        currentPosition.getData().add(currentPoint);
        
        // Style the current position point
        Platform.runLater(() -> {
            if (currentPoint.getNode() != null) {
                currentPoint.getNode().setStyle(
                    "-fx-background-color: gold; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-padding: 5px;"
                );
                currentPoint.getNode().setScaleX(1.5);
                currentPoint.getNode().setScaleY(1.5);
                
                Tooltip tooltip = new Tooltip(
                    String.format("Current Position\nTime: %s\nAzimuth: %.2f°\nElevation: %.2f°",
                        time.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        azimuth,
                        elevation
                    )
                );
                tooltip.setShowDelay(Duration.millis(50));
                Tooltip.install(currentPoint.getNode(), tooltip);
            }
        });
        
        // Update feedback points
        feedbackPoints.getData().clear();
        
        // Only show today's feedback points
        for (FeedbackEvent event : feedbackEvents) {
            if (event.timestamp.isAfter(today)) {
                XYChart.Data<Number, Number> point = new XYChart.Data<>(event.azimuth, event.elevation);
                
                // Set extra value for tooltip
                point.setExtraValue(String.format(
                    "Feedback: %s\nTime: %s\nAzimuth: %.2f°\nElevation: %.2f°\nPulses: %d\nIntensity: %d",
                    event.type,
                    event.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    event.azimuth,
                    event.elevation,
                    event.pulses,
                    event.intensity
                ));
                
                feedbackPoints.getData().add(point);
            }
        }
        
        // Style feedback points
        Platform.runLater(() -> {
            for (XYChart.Data<Number, Number> point : feedbackPoints.getData()) {
                if (point.getNode() != null) {
                    point.getNode().setStyle(
                        "-fx-background-color: red; " +
                        "-fx-background-radius: 6px;"
                    );
                    point.getNode().setScaleX(1.2);
                    point.getNode().setScaleY(1.2);
                    
                    Tooltip tooltip = new Tooltip((String) point.getExtraValue());
                    tooltip.setShowDelay(Duration.millis(50));
                    Tooltip.install(point.getNode(), tooltip);
                }
            }
        });
    }
}