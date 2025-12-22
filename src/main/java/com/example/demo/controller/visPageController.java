package com.example.demo.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.example.demo.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tooltip;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.util.Duration;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.scene.Node;

public class visPageController {

	private final ObservableList<User> userData = FXCollections.observableArrayList();

	@FXML
	private AnchorPane chartAnchor;
	@FXML
	private LineChart<String, Number> heartRateChart;
	@FXML
	private ComboBox<String> userSelector;
	@FXML
	private ComboBox<String> useCaseSelector;
	@FXML
	private ComboBox<String> timeRangeSelector;
	@FXML
	private Button backButton;
	@FXML
	private ImageView sunIcon; // Add this to your FXML
	@FXML
	private Label elevationLabel; // Add this to your FXML
	@FXML
	private Label azimuthLabel; // Add this to your FXML

	@FXML
	private Button dailyHeartPdfBtn;
	@FXML
	private Button generatePdfButton; // Add this to your FXML

	private ScheduledExecutorService compassUpdateScheduler;
	private boolean isSunPositionSelected = false;

	@FXML
	private void initialize() {
		loadUserData();
		populateUseCaseSelector();
		populateUserSelector();

		Tooltip.install(backButton, new Tooltip("Back to Main Menu"));

		// Initialize sun icon if available
		if (sunIcon != null) {
			try {
				Image sunImage = new Image(getClass().getResourceAsStream("/com/example/demo/images/sun.png"));
				sunIcon.setImage(sunImage);
				sunIcon.setVisible(false); // Initially hidden until sun position is selected
			} catch (Exception e) {
				System.err.println("Could not load sun icon: " + e.getMessage());
			}
		}

		if (elevationLabel != null)
			elevationLabel.setVisible(false);
		if (azimuthLabel != null)
			azimuthLabel.setVisible(false);

		userSelector.setOnAction(e -> updateChart());
		useCaseSelector.setOnAction(e -> {
			String selected = useCaseSelector.getValue();
			isSunPositionSelected = "SunAzimuth".equals(selected);
			updateSunPositionComponents(isSunPositionSelected);
			updateChart();
		});

		timeRangeSelector.getItems().addAll("Last 24 Hours", "Last 7 Days", "Last 30 Days", "All Time");
		timeRangeSelector.setValue("All Time");
		timeRangeSelector.setOnAction(e -> updateChart());

		if (generatePdfButton != null) {
			generatePdfButton.setOnAction(e -> generatePdfLog());
		}
	}

	private void updateSunPositionComponents(boolean showSunComponents) {
		if (sunIcon != null)
			sunIcon.setVisible(showSunComponents);
		if (elevationLabel != null)
			elevationLabel.setVisible(showSunComponents);
		if (azimuthLabel != null)
			azimuthLabel.setVisible(showSunComponents);

		if (showSunComponents) {
			startCompassUpdates();
		} else {
			stopCompassUpdates();
		}
	}

	private void startCompassUpdates() {
		if (compassUpdateScheduler != null && !compassUpdateScheduler.isShutdown()) {
			compassUpdateScheduler.shutdownNow();
		}

		compassUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
		compassUpdateScheduler.scheduleAtFixedRate(this::updateSunCompass, 0, 30, TimeUnit.SECONDS);
	}

	private void stopCompassUpdates() {
		if (compassUpdateScheduler != null && !compassUpdateScheduler.isShutdown()) {
			compassUpdateScheduler.shutdownNow();
			compassUpdateScheduler = null;
		}
	}

	private void updateSunCompass() {// to be implemented
//		try {
//			@SuppressWarnings("deprecation")
//			URL url = new URL("http://localhost:1880/get-sun");
//			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//			conn.setRequestMethod("GET");
//
//			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//			StringBuilder response = new StringBuilder();
//			String line;
//			while ((line = reader.readLine()) != null)
//				response.append(line);
//			reader.close();
//
//			ObjectMapper mapper = new ObjectMapper();
//			JsonNode root = mapper.readTree(response.toString());
//			double azimuth = root.get("azimuth").asDouble();
//			double elevation = root.get("elevation").asDouble();
//
//			javafx.application.Platform.runLater(() -> {
//				rotateSunIcon(azimuth);
//				if (elevationLabel != null)
//					elevationLabel.setText(String.format("Elevation: %.1f°", elevation));
//				if (azimuthLabel != null)
//					azimuthLabel.setText(String.format("Azimuth: %.1f°", azimuth));
//			});
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	private void rotateSunIcon(double azimuth) {
		if (sunIcon != null) {
			Rotate rotate = new Rotate(azimuth, sunIcon.getFitWidth() / 2, sunIcon.getFitHeight() / 2);
			sunIcon.getTransforms().clear();
			sunIcon.getTransforms().add(rotate);
		}
	}

	private void loadUserData() {
		userData.clear();

		try {
			@SuppressWarnings("deprecation")
			URL url = new URL("http://localhost:5678/webhook/get-users");
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
			JsonNode root = mapper.readTree(response.toString());

			for (JsonNode userNode : root) {
				int id = userNode.get("userid").asInt();
				String firstName = userNode.get("fname").asText();
				String lastName = userNode.get("lname").asText();
				userData.add(new User(id, firstName, lastName));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void populateUserSelector() {
		userSelector.getItems().clear();
		for (User u : userData) {
			userSelector.getItems().add(u.getUserID() + " - " + u.getFName() + " " + u.getLName());
		}
	}

	private void populateUseCaseSelector() {
		useCaseSelector.getItems().addAll("HeartRate", "SunAzimuth", "MoonAzimuth");
	}

	@FXML
	private void handleBackButton(ActionEvent event) {
		// Stop compass updates when navigating away
		stopCompassUpdates();

		try {
			// Load FXML using default controller defined in FXML file
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/com/example/demo/view/ResearcherInterface.fxml"));
			Parent root = loader.load();

			// Get current stage from the event source
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void plotSunPosition(JsonNode dataNode, int selectedUserID, String timeRange) {
		heartRateChart.setTitle("Sun Azimuth Over Time");
		heartRateChart.getData().clear();

		XYChart.Series<String, Number> azimuthSeries = new XYChart.Series<>();
		azimuthSeries.setName("Sun Azimuth");

		XYChart.Series<String, Number> feedbackSeries = new XYChart.Series<>();
		feedbackSeries.setName("Feedback for User " + selectedUserID);

		DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		System.out.println("Total records received: " + dataNode.size());
		int totalPoints = 0;

		for (JsonNode node : dataNode) {
			System.out.println(node + " HERE!");
			int sid = node.get("sensorid").asInt();
			if (sid != 23)
				continue; // Only process sun position entries

			int uid = node.get("userid").asInt();
			if (uid != selectedUserID)
				continue; // Only process selected user's data

			totalPoints++;

			String timeStr = node.get("time").asText();
			Instant instant = Instant.parse(timeStr);
			LocalDateTime dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
			String timeFormatted = dateTime.format(displayFormatter);

			double azimuth = node.get("value").asDouble();

			if (totalPoints <= 5) {
				System.out.println("Adding sun position: Time=" + timeFormatted + ", Azimuth=" + azimuth);
			}

			azimuthSeries.getData().add(new XYChart.Data<>(timeFormatted, azimuth));

			// Add feedback only if it exists
			String alertType = node.hasNonNull("alert_type") ? node.get("alert_type").asText() : null;
			if (alertType != null) {
				XYChart.Data<String, Number> feedbackPoint = new XYChart.Data<>(timeFormatted, azimuth);

				StringBuilder tooltipText = new StringBuilder();
				tooltipText.append("Feedback Triggered\n");
				tooltipText.append("Time: ").append(dateTime).append("\n");
				tooltipText.append("Azimuth: ").append(azimuth).append("\n");
				tooltipText.append("Type: ").append(alertType).append("\n");

				if (node.has("pulses"))
					tooltipText.append("Pulses: ").append(node.get("pulses").asInt()).append("\n");
				if (node.has("intensity"))
					tooltipText.append("Intensity: ").append(node.get("intensity").asInt()).append("\n");

				feedbackPoint.setExtraValue(tooltipText.toString());
				feedbackSeries.getData().add(feedbackPoint);
			}
		}

		System.out.println("Total matching sun position data points: " + totalPoints);

		// Check if we have data to display
		if (azimuthSeries.getData().isEmpty()) {
			System.out.println("WARNING: No data points to display!");

			Label noDataLabel = new Label("No data available for the selected time range.");
			noDataLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
			noDataLabel.setLayoutX(50);
			noDataLabel.setLayoutY(50);

			// Remove previous "no data" labels if any
			chartAnchor.getChildren()
					.removeIf(node -> node instanceof Label && ((Label) node).getText().startsWith("No data"));

			chartAnchor.getChildren().add(noDataLabel);
			return;
		}

		heartRateChart.getData().add(azimuthSeries);
		if (!feedbackSeries.getData().isEmpty()) {
			heartRateChart.getData().add(feedbackSeries);
		}

		// Configure dynamic chart width based on data points
		configureChartDisplay(azimuthSeries.getData().size());

		javafx.application.Platform.runLater(() -> {
			for (XYChart.Data<String, Number> point : azimuthSeries.getData()) {
				if (point.getNode() != null) {
					Tooltip tooltip = new Tooltip("Azimuth: " + point.getYValue());
					tooltip.setShowDelay(Duration.millis(50));
					Tooltip.install(point.getNode(), tooltip);
					point.getNode().setStyle("-fx-background-color: #4682B4; -fx-background-radius: 3px;");
					point.getNode().setScaleX(1.2);
					point.getNode().setScaleY(1.2);
				}
			}
			for (XYChart.Data<String, Number> point : feedbackSeries.getData()) {
				if (point.getNode() != null) {
					Tooltip tooltip = new Tooltip((String) point.getExtraValue());
					tooltip.setShowDelay(Duration.millis(50));
					Tooltip.install(point.getNode(), tooltip);
					point.getNode().setStyle("-fx-background-color: red; -fx-background-radius: 5px;");
					point.getNode().setScaleX(1.5);
					point.getNode().setScaleY(1.5);
				}
			}
		});
	}

	private void updateChart() {
		String userSelection = userSelector.getValue();
		String useCase = useCaseSelector.getValue();
		if (userSelection == null || useCase == null)
			return;

		int selectedUserID = Integer.parseInt(userSelection.split(" ")[0]);
		int selectedSensorID = getSensorIdByName(useCaseSelector.getValue());
		if (selectedSensorID == -1) {
			System.out.println("Sensor type not found: " + useCaseSelector.getValue());
			return;
		}

		String timeRange = timeRangeSelector.getValue();
		System.out.println("Selected time range: " + timeRange);

		// Clear existing data before fetching new data
		heartRateChart.getData().clear();

		try {
			String urlStr = "http://localhost:5678/webhook/sensor-data?range="
					+ URLEncoder.encode(timeRange, StandardCharsets.UTF_8) + "&alert_type="
					+ URLEncoder.encode(useCaseSelector.getValue(), StandardCharsets.UTF_8);
			System.out.println("API URL: " + urlStr);

			@SuppressWarnings("deprecation")
			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			int responseCode = conn.getResponseCode();
			System.out.println("Response code: " + responseCode);

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			reader.close();

			String responseStr = response.toString();
			System.out.println("API Response (truncated): "
					+ (responseStr.length() > 200 ? responseStr.substring(0, 200) + "..." : responseStr));

			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(responseStr);

			JsonNode dataNode = rootNode;
			if (rootNode.has("data")) {
				dataNode = rootNode.get("data");
			}

			if (dataNode.isArray()) {
				System.out.println("Number of data points returned: " + dataNode.size());

				// Process data based on selected use case
				if (useCase.equals("SunAzimuth")) {
					plotSunPosition(dataNode, selectedUserID, timeRange);
				} else if (useCase.equals("HeartRate")) {
					plotHeartRate(dataNode, selectedUserID, selectedSensorID, timeRange);
				} else if (useCase.equals("MoonAzimuth")) {
					plotMoonPosition(dataNode, selectedUserID, selectedSensorID, timeRange);

				}

			} else {
				System.out.println("No data returned or invalid format: " + response);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void plotHeartRate(JsonNode dataNode, int selectedUserID, int selectedSensorID, String timeRange) {
		heartRateChart.setTitle("Heart Rate Over Time");

		XYChart.Series<String, Number> heartRateSeries = new XYChart.Series<>();
		heartRateSeries.setName("HeartRate (User " + selectedUserID + ")");

		XYChart.Series<String, Number> feedbackPoints = new XYChart.Series<>();
		feedbackPoints.setName("Feedback Moments");

		DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		// Remove previous "no data" labels if any
		chartAnchor.getChildren()
				.removeIf(node -> node instanceof Label && ((Label) node).getText().startsWith("No data"));

		for (JsonNode node : dataNode) {
			int nodeUserID = node.get("userid").asInt();
			int nodeSensorID = node.get("sensorid").asInt();

			// Only process the rows that match selected user and sensor
			if (nodeUserID != selectedUserID || nodeSensorID != selectedSensorID) {
				continue;
			}

			// Parse time
			String timeStr = node.get("time").asText();
			Instant instant = Instant.parse(timeStr);
			LocalDateTime dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
			String formattedTime = dateTime.format(displayFormatter);

			double value = node.get("value").asDouble();

			XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(formattedTime, value);
			heartRateSeries.getData().add(dataPoint);

			// Add feedback points if they exist
			String alertType = node.hasNonNull("alert_type") ? node.get("alert_type").asText() : null;
			if (alertType != null) {
				XYChart.Data<String, Number> feedbackPoint = new XYChart.Data<>(formattedTime, value);

				StringBuilder tooltipText = new StringBuilder();
				tooltipText.append(String.format("Time: %s\n", dateTime));
				tooltipText.append(String.format("Value: %.1f\n", value));
				tooltipText.append("Feedback: Yes\n");
				tooltipText.append(String.format("Type: %s\n", alertType));

				if (node.hasNonNull("pulses"))
					tooltipText.append(String.format("Pulses: %d\n", node.get("pulses").asInt()));
				if (node.hasNonNull("intensity"))
					tooltipText.append(String.format("Intensity: %d\n", node.get("intensity").asInt()));
				if (node.hasNonNull("duration"))
					tooltipText.append(String.format("Duration: %d ms\n", node.get("duration").asInt()));
				if (node.hasNonNull("interval"))
					tooltipText.append(String.format("Interval: %d ms\n", node.get("interval").asInt()));

				feedbackPoint.setExtraValue(tooltipText.toString());
				feedbackPoints.getData().add(feedbackPoint);
			}
		}

		// Check if we have data to display
		if (heartRateSeries.getData().isEmpty()) {
			System.out.println("WARNING: No data points to display!");

			Label noDataLabel = new Label("No data available for the selected time range.");
			noDataLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
			noDataLabel.setLayoutX(50);
			noDataLabel.setLayoutY(50);

			chartAnchor.getChildren().add(noDataLabel);
			return;
		}

		// Add data to the chart
		heartRateChart.getData().add(heartRateSeries);
		if (!feedbackPoints.getData().isEmpty()) {
			heartRateChart.getData().add(feedbackPoints);
		}

		// Configure chart display
		configureChartDisplay(heartRateSeries.getData().size());

		// Install tooltips
		javafx.application.Platform.runLater(() -> {
			for (XYChart.Data<String, Number> data : heartRateSeries.getData()) {
				if (data.getNode() != null) {
					String tooltipText = String.format("Value: %.1f", data.getYValue().doubleValue());
					Tooltip tooltip = new Tooltip(tooltipText);
					tooltip.setShowDelay(Duration.millis(50));
					Tooltip.install(data.getNode(), tooltip);
					data.getNode().setStyle("-fx-background-color: #4682B4; -fx-background-radius: 3px;");
					data.getNode().setScaleX(1.2);
					data.getNode().setScaleY(1.2);
				}
			}

			for (XYChart.Data<String, Number> data : feedbackPoints.getData()) {
				if (data.getNode() != null) {
					String tooltipText = (String) data.getExtraValue();
					Tooltip tooltip = new Tooltip(tooltipText);
					tooltip.setShowDelay(Duration.millis(50));
					Tooltip.install(data.getNode(), tooltip);
					data.getNode().setStyle("-fx-background-color: red; -fx-background-radius: 5px;");
					data.getNode().setScaleX(1.5);
					data.getNode().setScaleY(1.5);
				}
			}
		});
	}

	private void plotMoonPosition(JsonNode dataNode, int selectedUserID, int selectedSensorID, String timeRange) {
		heartRateChart.setTitle("Moon Azimuth Over Time");
		heartRateChart.getData().clear();

		XYChart.Series<String, Number> moonSeries = new XYChart.Series<>();
		moonSeries.setName("Moon Azimuth");

		XYChart.Series<String, Number> feedbackSeries = new XYChart.Series<>();
		feedbackSeries.setName("Feedback for User " + selectedUserID);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		// Remove previous "no data" labels if any
		chartAnchor.getChildren()
				.removeIf(node -> node instanceof Label && ((Label) node).getText().startsWith("No data"));

		for (JsonNode node : dataNode) {
			int sid = node.get("sensorid").asInt();
			if (sid != selectedSensorID)
				continue;

			int uid = node.get("userid").asInt();
			if (uid != selectedUserID)
				continue;

			String timeStr = node.get("time").asText();
			Instant instant = Instant.parse(timeStr);
			LocalDateTime dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
			String timeFormatted = dateTime.format(formatter);

			double azimuth = node.get("value").asDouble();
			XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(timeFormatted, azimuth);
			moonSeries.getData().add(dataPoint);

			if (node.hasNonNull("alert_type")) {
				XYChart.Data<String, Number> feedbackPoint = new XYChart.Data<>(timeFormatted, azimuth);
				StringBuilder tooltip = new StringBuilder("Feedback Triggered\n");
				tooltip.append("Time: ").append(timeFormatted).append("\n");
				tooltip.append("Azimuth: ").append(azimuth).append("\n");
				tooltip.append("Type: ").append(node.get("alert_type").asText()).append("\n");

				if (node.has("pulses"))
					tooltip.append("Pulses: ").append(node.get("pulses").asInt()).append("\n");
				if (node.has("intensity"))
					tooltip.append("Intensity: ").append(node.get("intensity").asInt()).append("\n");

				feedbackPoint.setExtraValue(tooltip.toString());
				feedbackSeries.getData().add(feedbackPoint);
			}
		}

		if (moonSeries.getData().isEmpty()) {
			Label noDataLabel = new Label("No data available for the selected time range.");
			noDataLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
			noDataLabel.setLayoutX(50);
			noDataLabel.setLayoutY(50);
			chartAnchor.getChildren().add(noDataLabel);
			return;
		}

		heartRateChart.getData().add(moonSeries);
		if (!feedbackSeries.getData().isEmpty()) {
			heartRateChart.getData().add(feedbackSeries);
		}

		configureChartDisplay(moonSeries.getData().size());

		Platform.runLater(() -> {
			for (XYChart.Data<String, Number> point : moonSeries.getData()) {
				if (point.getNode() != null) {
					Tooltip tooltip = new Tooltip("Azimuth: " + point.getYValue());
					tooltip.setShowDelay(Duration.millis(50));
					Tooltip.install(point.getNode(), tooltip);
					point.getNode().setStyle("-fx-background-color: #4682B4; -fx-background-radius: 3px;");
					point.getNode().setScaleX(1.2);
					point.getNode().setScaleY(1.2);
				}
			}

			for (XYChart.Data<String, Number> point : feedbackSeries.getData()) {
				if (point.getNode() != null) {
					Tooltip tooltip = new Tooltip((String) point.getExtraValue());
					tooltip.setShowDelay(Duration.millis(50));
					Tooltip.install(point.getNode(), tooltip);
					point.getNode().setStyle("-fx-background-color: red; -fx-background-radius: 5px;");
					point.getNode().setScaleX(1.5);
					point.getNode().setScaleY(1.5);
				}
			}
		});
	}

//FIXED TIMES IN X-AXIS , NOT CROWDED
	private void configureChartDisplay(int dataCount) {
		int minWidthPerDataPoint = 20;
		int chartWidth = Math.max(800, dataCount * minWidthPerDataPoint);

		// Use the same Y-axis for both charts
		NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("BPM");
		yAxis.setAutoRanging(true);

		// Create left chart with only Y-axis (no data)
		LineChart<String, Number> yAxisChart = new LineChart<>(new CategoryAxis(), yAxis);
		yAxisChart.setAnimated(false);
		yAxisChart.setLegendVisible(false);
		yAxisChart.setVerticalGridLinesVisible(false);
		yAxisChart.setHorizontalGridLinesVisible(false);
		yAxisChart.setCreateSymbols(false);
		yAxisChart.setPrefWidth(80);
		yAxisChart.setMinWidth(80);
		yAxisChart.setMaxWidth(80);
		yAxisChart.setStyle("-fx-padding: 0 5 0 10; -fx-background-color: transparent;");

		// Remove plot area from yAxisChart
		yAxisChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
		yAxisChart.setStyle("-fx-background-color: transparent;");

		// Use your actual heartRateChart (already populated)
		heartRateChart.setAnimated(false);
		heartRateChart.setLegendVisible(false);
		heartRateChart.setPrefWidth(chartWidth);
		heartRateChart.setMinWidth(chartWidth);
		heartRateChart.setCreateSymbols(true); // Or false if you want smooth lines
		((CategoryAxis) heartRateChart.getXAxis()).setTickLabelRotation(45);

		// Wrap heartRateChart in scroll pane
		ScrollPane scrollPane = new ScrollPane(heartRateChart);
		scrollPane.setFitToHeight(true);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
		scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		scrollPane.setPrefViewportWidth(800);

		// Remove previous content
		chartAnchor.getChildren().clear();

		// Create HBox with fixed Y-axis and scrollable chart
		HBox chartRow = new HBox();
		chartRow.getChildren().addAll(yAxisChart, scrollPane);

		// Add to AnchorPane
		chartAnchor.getChildren().add(chartRow);
		AnchorPane.setTopAnchor(chartRow, 40.0);
		AnchorPane.setLeftAnchor(chartRow, 10.0);
		AnchorPane.setRightAnchor(chartRow, 10.0);
		AnchorPane.setBottomAnchor(chartRow, 10.0);

		// Optional: redraw tick labels for X-axis
		Platform.runLater(() -> {
			Set<Node> labelNodes = ((CategoryAxis) heartRateChart.getXAxis()).lookupAll(".axis .tick-label");
			if (labelNodes != null && !labelNodes.isEmpty()) {
				List<Text> labels = labelNodes.stream().filter(node -> node instanceof Text).map(node -> (Text) node)
						.collect(Collectors.toList());

				int showEveryNth = Math.max(1, labels.size() / 15);
				for (int i = 0; i < labels.size(); i++) {
					labels.get(i).setVisible(i % showEveryNth == 0 || i == 0 || i == labels.size() - 1);
				}
			}
		});
	}

	@SuppressWarnings("deprecation")
	public void generateDailyHeartRatePdfForAllUsers() {
		DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate today = LocalDate.now();
		for (User user : userData) {
			int userID = user.getUserID();
			try {
				// Fetch user heart rate data
				String urlStr = "http://localhost:5678/webhook/sensor-data?userId=" + userID;
				URL url = new URL(urlStr);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");

				// Check response code first
				if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
					System.err.println("HTTP request failed with response code: " + conn.getResponseCode());
					continue;
				}

				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder response = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					response.append(line);
				}
				reader.close();

				// Parse the JSON response
				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readTree(response.toString());

				// Create PDF file
				String fileName = "HeartRateLog_User" + userID + "_" + today.format(dateOnly) + ".pdf";
				Document document = new Document();
				PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
				document.open();

				// Add content to PDF
				Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
				document.add(new Paragraph("Daily Heart Rate Log - User ID: " + userID, titleFont));
				document.add(new Paragraph("Date: " + today.format(dateOnly)));
				document.add(new Paragraph(" "));

				// Create table
				PdfPTable table = new PdfPTable(7); // 7 columns
				table.setWidthPercentage(100);
				table.addCell("Time");
				table.addCell("Heart Rate (BPM)");
				table.addCell("Alert Type");
				table.addCell("Pulses");
				table.addCell("Intensity");
				table.addCell("Duration");
				table.addCell("Max Value");

				// Add data to table
				int count = 0;
				if (root.isArray()) {
					for (JsonNode node : root) {
						try {
							// Filter for heart rate data (sensorid 11) and today's date
							if (!node.has("sensorid") || node.get("sensorid").asInt() != 11) {
								continue;
							}

							if (!node.has("time") || !node.has("value")) {
								System.out.println("Node missing time or value field: " + node);
								continue;
							}

							Instant instant = Instant.parse(node.get("time").asText());
							LocalDateTime dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
							LocalDate recordDate = dateTime.toLocalDate();

							if (recordDate.equals(today)) {
								table.addCell(dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
								table.addCell(node.has("value") ? String.valueOf(node.get("value").asInt()) : "N/A");
								table.addCell(node.has("alert_type") ? node.get("alert_type").asText() : "N/A");
								table.addCell(node.has("pulses") ? String.valueOf(node.get("pulses").asInt()) : "N/A");
								table.addCell(
										node.has("intensity") ? String.valueOf(node.get("intensity").asInt()) : "N/A");
								table.addCell(
										node.has("duration") ? String.valueOf(node.get("duration").asInt()) : "N/A");
								table.addCell(
										node.has("maxvalue") ? String.valueOf(node.get("maxvalue").asInt()) : "N/A");
								count++;

							}
						} catch (Exception e) {
							System.err.println("Error processing node " + node + ": " + e.getMessage());
						}
					}
				}

				// Add table to document or show "no data" message
				if (count == 0) {
					document.add(new Paragraph("No heart rate data found for today."));
					// Debug information in PDF
					document.add(new Paragraph("Debug Info:"));
					document.add(new Paragraph("Today's date: " + today.format(dateOnly)));
					document.add(new Paragraph("Total records received: " + root.size()));
					document.add(new Paragraph("Filtered for sensor ID: 11 (Heart Rate)"));
				} else {
					document.add(table);
					document.add(new Paragraph("Total heart rate records for today: " + count));
				}

				// Close the document
				document.close();
				writer.close();

				System.out.println("PDF saved: " + new File(fileName).getAbsolutePath());

				// Open PDF in browser
				try {
					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().open(new File(fileName));
					} else {
						// Fallback for systems without Desktop support
						String osName = System.getProperty("os.name").toLowerCase();
						if (osName.contains("win")) {
							Runtime.getRuntime().exec(
									"rundll32 url.dll,FileProtocolHandler " + new File(fileName).getAbsolutePath());
						} else if (osName.contains("mac")) {
							Runtime.getRuntime().exec("open " + new File(fileName).getAbsolutePath());
						} else if (osName.contains("nix") || osName.contains("nux")) {
							// Try common Linux browsers
							String[] browsers = { "google-chrome", "firefox", "mozilla", "epiphany", "konqueror",
									"netscape", "opera", "links", "lynx" };

							String browser = null;
							for (String b : browsers) {
								if (Runtime.getRuntime().exec(new String[] { "which", b }).waitFor() == 0) {
									browser = b;
									break;
								}
							}

							if (browser != null) {
								Runtime.getRuntime()
										.exec(new String[] { browser, new File(fileName).getAbsolutePath() });
							} else {
								System.out.println("Could not find web browser to open PDF");
							}
						}
					}
				} catch (Exception e) {
					System.out.println("Could not open PDF in browser: " + e.getMessage());
				}
			} catch (Exception e) {
				System.err.println("Error processing user " + userID + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public int getSensorIdByName(String name) {
		try {
			@SuppressWarnings("deprecation")
			URL url = new URL("http://localhost:5678/webhook/get-sensor-types");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			if (conn.getResponseCode() != 200) {
				System.out.println("HTTP error: " + conn.getResponseCode());
				return -1;
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				response.append(line);
			}
			in.close();

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response.toString());

			Map<String, Integer> sensorMap = new HashMap<>();
			for (JsonNode node : root) {
				String sensorName = node.get("name").asText();
				int sensorId = node.get("sensorid").asInt();
				sensorMap.put(sensorName, sensorId);
			}

			return sensorMap.getOrDefault(name, -1);

		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	@FXML
	private void generatePdfLog() {
		String userSelection = userSelector.getValue();
		String useCase = useCaseSelector.getValue();

		if (userSelection == null || useCase == null) {
			System.out.println("Please select a user and use case.");
			return;
		}

		int userID = Integer.parseInt(userSelection.split(" ")[0]);
		int sensorID = getSensorIdByName(useCase);

		String timeRange = timeRangeSelector.getValue();

		try {
			String urlStr = "http://localhost:5678/webhook/sensor-data?userId=" + userID + "&sensorId=" + sensorID + "&timeRange="
					+ timeRange.replace(" ", "%20");
			@SuppressWarnings("deprecation")
			URL url = new URL(urlStr);
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
			JsonNode root = mapper.readTree(response.toString());

			Document document = new Document();
			String fileName = "User_" + userID + "" + useCase.replace(" ", "") + "_log.pdf";
			PdfWriter.getInstance(document, new FileOutputStream(fileName));
			document.open();

			Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
			Font headingFont = new Font(Font.HELVETICA, 12, Font.BOLD);

			Paragraph title = new Paragraph("User Data Report", titleFont);
			title.setAlignment(Element.ALIGN_CENTER);
			document.add(title);
			document.add(new Paragraph(" ")); // Spacer

			document.add(new Paragraph("User ID: " + userID, headingFont));
			document.add(new Paragraph("Use Case: " + useCase, headingFont));
			document.add(new Paragraph("Time Range: " + timeRange, headingFont));
			document.add(new Paragraph("Generated: " + LocalDateTime.now(), headingFont));
			document.add(new Paragraph(" ")); // Spacer

			PdfPTable table = new PdfPTable(7); // 7 columns
			table.setWidthPercentage(100);

			String[] headers = { "Time", "Value", "Feedback", "Pulses", "Intensity", "Duration (ms)", "Interval (ms)" };
			for (String header : headers) {
				table.addCell(new Paragraph(header, headingFont));
			}

			int rowCount = 0;
			if (root.isArray()) {
				for (JsonNode node : root) {
					rowCount++;

					String time = node.hasNonNull("time") ? node.get("time").asText() : "N/A";
					double value = node.hasNonNull("value") ? node.get("value").asDouble() : 0.0;
					String alertType = node.hasNonNull("alert_type") ? node.get("alert_type").asText() : null;

					table.addCell(time);
					table.addCell(String.valueOf(value));
					table.addCell(alertType != null ? "Yes" : "No");
					table.addCell(node.hasNonNull("pulses") ? String.valueOf(node.get("pulses").asInt()) : "N/A");
					table.addCell(node.hasNonNull("intensity") ? String.valueOf(node.get("intensity").asInt()) : "N/A");
					table.addCell(node.hasNonNull("duration") ? String.valueOf(node.get("duration").asInt()) : "N/A");
					table.addCell(node.hasNonNull("interval") ? String.valueOf(node.get("interval").asInt()) : "N/A");
				}
			}

			if (rowCount == 0) {
				document.add(new Paragraph("No data found for the selected user and use case."));
			} else {
				document.add(table);
			}

			document.close();

			File pdfFile = new File(fileName).getAbsoluteFile();
			String fullPath = pdfFile.getAbsolutePath();

			System.out.println("PDF generated: " + fullPath);

			javafx.application.Platform.runLater(() -> {
				javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
						javafx.scene.control.Alert.AlertType.INFORMATION);
				alert.setTitle("PDF Generated");
				alert.setHeaderText("PDF Report Created Successfully");
				alert.setContentText("The report has been saved to:\n" + fullPath);

				javafx.scene.control.Hyperlink link = new javafx.scene.control.Hyperlink("Open PDF");
				link.setOnAction(e -> {
					try {
						if (java.awt.Desktop.isDesktopSupported()) {
							java.awt.Desktop.getDesktop().open(pdfFile);
						}
					} catch (Exception ex) {
						System.out.println("Cannot open PDF: " + ex.getMessage());
					}
				});

				javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
				content.getChildren().addAll(new javafx.scene.control.Label("The report has been saved to:"),
						new javafx.scene.control.Label(fullPath), link);
				alert.getDialogPane().setContent(content);

				alert.showAndWait();
			});

		} catch (Exception e) {
			e.printStackTrace();
			javafx.application.Platform.runLater(() -> {
				javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
						javafx.scene.control.Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Failed to Generate PDF");
				alert.setContentText("Error: " + e.getMessage());
				alert.showAndWait();
			});
		}
	}

}