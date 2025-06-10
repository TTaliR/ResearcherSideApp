package com.example.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class SunPositionController {

    @FXML private LineChart<String, Number> sunChart;
    @FXML private ImageView sunIcon;
    @FXML private Label elevationLabel;
    @FXML private Label azimuthLabel;


    private ScheduledExecutorService chartScheduler;
    private ScheduledExecutorService compassScheduler;
    private int selectedUserID;

    public void initializeLiveView(int userId) {
        this.selectedUserID = userId;
        // Load the sun icon image (ensure image is in resources/images/)
        Image sunImage = new Image(getClass().getResourceAsStream("/com/example/demo/images/sun.png"));
        sunIcon.setImage(sunImage);

        // Start updating chart every 15 seconds
        chartScheduler = Executors.newSingleThreadScheduledExecutor();
        chartScheduler.scheduleAtFixedRate(() -> Platform.runLater(this::fetchAndUpdateChart), 0, 15, TimeUnit.SECONDS);

        // Start compass update every 60 seconds
        compassScheduler = Executors.newSingleThreadScheduledExecutor();
        compassScheduler.scheduleAtFixedRate(this::updateCompass, 0, 60, TimeUnit.SECONDS);
    }

    private void fetchAndUpdateChart() {
        try {
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

            sunChart.getData().clear();
            XYChart.Series<String, Number> azimuthSeries = new XYChart.Series<>();
            azimuthSeries.setName("Sun Azimuth");

            XYChart.Series<String, Number> feedbackSeries = new XYChart.Series<>();
            feedbackSeries.setName("Feedback for User " + selectedUserID);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            for (JsonNode node : dataNode) {
                if (node.get("sensorid").asInt() != 2) continue;

                LocalDateTime dateTime = Instant.parse(node.get("time").asText())
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();

                String formattedTime = dateTime.format(formatter);
                double azimuth = node.get("value").asDouble();

                XYChart.Data<String, Number> point = new XYChart.Data<>(formattedTime, azimuth);
                azimuthSeries.getData().add(point);

                if (node.get("userid").asInt() == selectedUserID && node.hasNonNull("alert_type")) {
                    XYChart.Data<String, Number> feedback = new XYChart.Data<>(formattedTime, azimuth);

                    StringBuilder tooltipText = new StringBuilder();
                    tooltipText.append("Feedback Triggered\n");
                    tooltipText.append("Time: ").append(dateTime).append("\n");
                    tooltipText.append("Azimuth: ").append(azimuth).append("\n");
                    tooltipText.append("Type: ").append(node.get("alert_type").asText()).append("\n");

                    if (node.has("pulses")) tooltipText.append("Pulses: ").append(node.get("pulses").asInt()).append("\n");
                    if (node.has("intensity")) tooltipText.append("Intensity: ").append(node.get("intensity").asInt()).append("\n");

                    feedback.setExtraValue(tooltipText.toString());
                    feedbackSeries.getData().add(feedback);
                }
            }

            sunChart.getData().add(azimuthSeries);
            if (!feedbackSeries.getData().isEmpty()) sunChart.getData().add(feedbackSeries);

            Platform.runLater(() -> {
                for (XYChart.Data<String, Number> d : azimuthSeries.getData()) {
                    if (d.getNode() != null) {
                        Tooltip.install(d.getNode(), new Tooltip("Azimuth: " + d.getYValue()));
                    }
                }
                for (XYChart.Data<String, Number> d : feedbackSeries.getData()) {
                    if (d.getNode() != null) {
                        Tooltip tip = new Tooltip((String) d.getExtraValue());
                        tip.setShowDelay(Duration.millis(50));
                        Tooltip.install(d.getNode(), tip);
                        d.getNode().setStyle("-fx-background-color: red; -fx-background-radius: 5px;");
                        d.getNode().setScaleX(1.5);
                        d.getNode().setScaleY(1.5);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCompass() {
        try {
            URL url = new URL("http://localhost:1880/get-sun"); // your live API
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.toString());
            double azimuth = root.get("azimuth").asDouble();
            double elevation = root.get("elevation").asDouble();

            Platform.runLater(() -> {
                rotateSunIcon(azimuth);
                elevationLabel.setText(String.format("Elevation: %.1f°", elevation));
                azimuthLabel.setText(String.format("Azimuth: %.1f°", azimuth));

            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rotateSunIcon(double azimuth) {
        Rotate rotate = new Rotate(azimuth, sunIcon.getFitWidth() / 2, sunIcon.getFitHeight() / 2);
        sunIcon.getTransforms().clear();
        sunIcon.getTransforms().add(rotate);
    }

    public void onClose() {
        if (chartScheduler != null && !chartScheduler.isShutdown()) {
            chartScheduler.shutdownNow();
        }
        if (compassScheduler != null && !compassScheduler.isShutdown()) {
            compassScheduler.shutdownNow();
        }
    }
}
