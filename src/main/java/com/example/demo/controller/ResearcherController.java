package com.example.demo.controller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import com.example.demo.model.AzimuthRange;
import com.example.demo.model.HeartRateRange;
import com.example.demo.model.HeartRateRange.HeartRateThresholdMapping;
import com.example.demo.model.SunMoonThreshold;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class ResearcherController {
	@FXML
	private RadioButton heartRateRadio, sunPositionRadio, moonPositionRadio;

	@FXML
	private ComboBox<String> monitoringComboBox;
	//

	// FXML References
	@FXML
	private CheckBox sunPositionToggle;

	@FXML
	private CheckBox sunsetToggle;
	@FXML
	private TextField sunsetThreshold;
	@FXML
	private TextField heartRateThresholdField;

	@FXML
	private Button heartRateSaveButton;

	@FXML
	private CheckBox sunriseToggle;
	@FXML
	private TextField sunriseThreshold;

	@FXML
	private CheckBox moonPositionToggle;
	@FXML
	private TextField moonAzimuthThreshold;
	// @FXML
	// private TextField moonAltitudeThreshold;
	@FXML
	private Label activeParticipants;
	@FXML
	private Label dataPointsCollected;

	@FXML
	private LineChart<String, Number> dataCollectionChart;

	@FXML
	private VBox sunAzimuthRangesBox;
	@FXML
	private VBox moonAzimuthRangesBox;
	@FXML
	private TextField sunAzimuthMin;

	@FXML
	private TextField sunAzimuthMax;

	@FXML
	private Button sunDataSave;
	@FXML
	private VBox heartRateMappingsBox; // Container for dynamically added heart rate mapping inputs
	@FXML
	private Button addHeartRateMappingButton, saveHeartRateMappingsButton;
	String jsonInputString;

	private final List<HeartRateRange.HeartRateThresholdMapping> heartRateMappings = new ArrayList<>(); // will be deleted later
	private final List<HeartRateRange.HeartRateThresholdMapping> heartRateMappingsTest = new ArrayList<>(); // should be used.

	@FXML
	private VBox savedMappingsBox;

	private final List<AzimuthRange> sunRangeInputs = new ArrayList<>(); // will be deleted later
	private final List<AzimuthRange> sunRangeInputsTest = new ArrayList<>(); // should be used.
	private final List<AzimuthRange> moonRangeInputsTest = new ArrayList<>(); // should be used.


	private boolean fetchConfigurationClicked = false;

	public void initialize() {
		populateMonitorTypes();

	}

	/**
	 * Sends the currently selected monitoring type (e.g., HeartRate, SunAzimuth) to
	 * Node-RED via HTTP POST.
	 */
	@FXML
	public void sendMonitoringTypeToNodeRed() {
		String selectedType = monitoringComboBox.getSelectionModel().getSelectedItem();

		// Prepare the request payload with the selected monitoring type
		Map<String, String> requestBody = new HashMap<>();
		requestBody.put("monitoringType", selectedType);

		// Send to Node-RED
		if(postJsonToNodeRed(requestBody, "http://localhost:1880/set-monitoring-type",
				"Monitoring type sent successfully!")) {
			showAlert("Success", "Data Source was sucessfully sent");
		}
		else {
			showAlert("Failure", "An Error was encountered while sending the data source");

		}
		
	}

	/**
	 * Saves current sun azimuth data from UI and sends it to Node-RED. Also removes
	 * any invisible (deleted) entries from the VBox.
	 *
	 * @return true if the data was valid and successfully sent; false otherwise.
	 */
	private boolean sendSunRangeMappingsToNodeRed() {
		// Save data from UI fields into the sunRangeInputs list
		if (handleSaveSunData()) {
			// Wrap the data in a threshold container object
			SunMoonThreshold sunThreshold = new SunMoonThreshold();
			sunThreshold.setSunAzimuthRanges(new ArrayList<>(sunRangeInputsTest));

			// Debug: print heart rate mappings at this point
			for (HeartRateRange.HeartRateThresholdMapping p : heartRateMappingsTest) {
				System.out.println(p + " FROM SEND SUN RANGE MAPPING 1");
			}

			// Send to Node-RED via HTTP POST
			postJsonToNodeRed(sunThreshold, "http://localhost:1880/set-sun-azimuth-threshold",
					"Sun Azimuth Ranges sent successfully!");

			// Debug: print again after sending
			for (HeartRateRange.HeartRateThresholdMapping p : heartRateMappingsTest) {
				System.out.println(p + " FROM SEND SUN RANGE MAPPING 2");
			}

			// Physically remove invisible rows (marked inactive) from the UI container
			removeInvisibleNodes(sunAzimuthRangesBox);

			return true;
		}
		return false;
	}

	
	
	/**
	 * Saves current moon azimuth data from UI and sends it to Node-RED. Also removes
	 * any invisible (deleted) entries from the VBox.
	 *
	 * @return true if the data was valid and successfully sent; false otherwise.
	 */
	private boolean sendMoonRangeMappingsToNodeRed() {
		// Save data from UI fields into the sunRangeInputs list
		if (handleSaveMoonData()) {
			// Wrap the data in a threshold container object
			SunMoonThreshold moonThreshold = new SunMoonThreshold();
			moonThreshold.setMoonAzimuthRanges(new ArrayList<>(moonRangeInputsTest));

	

			// Send to Node-RED via HTTP POST
			postJsonToNodeRed(moonThreshold, "http://localhost:1880/set-moon-azimuth-threshold",
					"Moon Azimuth Ranges sent successfully!");

			// Debug: print again after sending
			for (HeartRateRange.HeartRateThresholdMapping p : heartRateMappingsTest) {
				System.out.println(p + " FROM SEND SUN RANGE MAPPING 2");
			}

			// Physically remove invisible rows (marked inactive) from the UI container
			removeInvisibleNodes(moonAzimuthRangesBox);

			return true;
		}
		return false;
	}
	
	/**
	 * Saves current heart rate mappings from UI and sends them to Node-RED. Also
	 * removes any invisible (deleted) rows from the VBox.
	 *
	 * @return true if the data was valid and successfully sent; false otherwise.
	 */
	private boolean sendHeartRateMappingsToNodeRed() {
		// Debug: log state before saving
		for (HeartRateRange.HeartRateThresholdMapping p : heartRateMappingsTest) {
			System.out.println(p + " FROM SEND HEART RATE MAPPINGS");
		}

		// Save data from UI fields into the heartRateMappings list
		if (saveHeartRateMappings()) {
			// Wrap the data in a threshold container object
			HeartRateRange heartThreshold = new HeartRateRange();
			heartThreshold.setThresholds(new ArrayList<>(heartRateMappingsTest));

			// Send to Node-RED via HTTP POST
			postJsonToNodeRed(heartThreshold, "http://localhost:1880/set-heart-rate-threshold",
					"Heart Rate mappings sent successfully!");

			// Remove invisible rows from the UI container
			removeInvisibleNodes(heartRateMappingsBox);

			return true;
		}
		return false;
	}

	/**
	 * Iterates through a JavaFX Pane container (like VBox) and safely removes any
	 * invisible nodes (e.g., deleted UI rows).
	 *
	 * This cleanup prevents invisible elements from persisting in the layout or
	 * affecting index-based logic.
	 *
	 * @param container the UI container to clean up
	 */
	private void removeInvisibleNodes(Pane container) {
		// Use an iterator to avoid ConcurrentModificationException during iteration
		Iterator<Node> iterator = container.getChildren().iterator();
		while (iterator.hasNext()) {
			Node node = iterator.next();
			if (!node.isVisible()) {
				iterator.remove(); // Safe removal of invisible node
			}
		}

		// Debug: log heart rate mappings after cleanup
		for (HeartRateRange.HeartRateThresholdMapping p : heartRateMappingsTest) {
			System.out.println(p + " FROM remove invisible nodes ");
		}
	}

	/**
	 * Aggregates and sends the current configurations (Sun Azimuth and Heart Rate
	 * mappings) to the backend (Node-RED). Displays user feedback via alerts and
	 * refreshes local data.
	 *
	 * @param event the ActionEvent from the UI (Save button clicked)
	 */
	@FXML
	public void saveConfigurationsToBackend(ActionEvent event) {
		try {
			// 🔍 Debug: Print all heart rate mappings before sending
			for (HeartRateRange.HeartRateThresholdMapping p : heartRateMappingsTest) {
				System.out.println(p + " FROM SAVE CONFIG");
			}

			// THIS METHOD NEEDS AN UPDATE WITH THE ALERTS(MOON NEEDS TO BE ADDED)
			
			// 🚀 Attempt to save both configurations
			Boolean sendSunRangeMappings = sendSunRangeMappingsToNodeRed();
			Boolean sendHeartRateMappings = sendHeartRateMappingsToNodeRed();
			Boolean sendMoonRangeMappings = sendMoonRangeMappingsToNodeRed();

			// ✅ Both configurations saved successfully
			if (sendSunRangeMappings && sendHeartRateMappings && sendMoonRangeMappings) {
				showAlert("Success", "Configurations saved successfully to Node-RED!");
				fetchCurrentConfigurationsWithoutMessage();

				// ✅ Only Sun configuration succeeded
			} else if (sendSunRangeMappings) {
				showAlert("Success", "Only Sun Rules Configurations saved successfully!");
				fetchCurrentConfigurationsWithoutMessage();

				// ✅ Only Heart configuration succeeded
			} else if (sendHeartRateMappings) {
				showAlert("Success", "Only Heart Rules Configurations saved successfully!");
				fetchCurrentConfigurationsWithoutMessage();

				// ❌ Neither succeeded
			} else {
				showAlert("Error", "An error occurred while saving configurations: ");
			}

		} catch (Exception e) {
			// ❗ Catch and report any unexpected errors
			e.printStackTrace();
			showAlert("Error", "An error occurred while saving configurations: " + e.getMessage());
		}
	}

	/**
	 * Creates a new row (HBox) for user input of a Sun Azimuth threshold. Includes
	 * fields for range and vibration configuration, duplicate prevention, and
	 * delete button.
	 *
	 * @param metricName label identifier (unused but kept for compatibility)
	 * @return a fully configured HBox with input fields and validation
	 */
	private HBox createSunAzimuthInput(String metricName) {
		HBox rangeInput = new HBox(10); // UI row with spacing between fields

		// 📥 Create input fields with tooltips and constraints
		TextField minField = createLabeledField("MinAzimuth", 80, "Minimum azimuth angle (0° = North, 180° = South)");
		TextField maxField = createLabeledField("MaxAzimuth", 80, "Maximum azimuth angle (0° = North, 180° = South)");
		TextField pulsesField = createLabeledField("Pulses", 80, "Number of vibration pulses.");
		ComboBox<Integer> intensityDropdown = createLabeledComboBox("Intensity", 100,
				"Intensity level of vibration (1=Low, 5=High).");
		TextField durationField = createLabeledField("Duration (ms)", 100,
				"Duration of each vibration pulse in milliseconds.");
		TextField intervalField = createLabeledField("Interval (ms)", 100,
				"Time between each vibration pulse in milliseconds.");

		// 🚫 Duplicate check: Prevent user from adding the exact same config
		Runnable checkDuplicate = () -> {
			String min = minField.getText(), max = maxField.getText(), pulses = pulsesField.getText();
			String duration = durationField.getText(), interval = intervalField.getText();
			Integer intensity = intensityDropdown.getValue();

			if (min.isEmpty() || max.isEmpty() || pulses.isEmpty() || duration.isEmpty() || interval.isEmpty()
					|| intensity == null)
				return;

			boolean isDuplicate = sunAzimuthRangesBox.getChildren().stream()
					.filter(node -> node instanceof HBox && node != rangeInput && node.isVisible())
					.map(node -> (HBox) node).anyMatch(existing -> {
						try {
							return min.equals(((TextField) existing.getChildren().get(0)).getText())
									&& max.equals(((TextField) existing.getChildren().get(1)).getText())
									&& pulses.equals(((TextField) existing.getChildren().get(2)).getText())
									&& intensity.equals(((ComboBox<?>) existing.getChildren().get(3)).getValue())
									&& duration.equals(((TextField) existing.getChildren().get(4)).getText())
									&& interval.equals(((TextField) existing.getChildren().get(5)).getText());
						} catch (Exception e) {
							return false;
						}
					});

			if (isDuplicate) {
				showAlert("Duplicate Mapping", "This Sun Azimuth range already exists.");
				intervalField.clear(); // Prompt user to edit
			}
		};

		// 🎯 Attach duplicate check to all relevant input fields
		Stream.of(minField, maxField, pulsesField, durationField, intervalField)
				.forEach(field -> field.focusedProperty().addListener((obs, was, now) -> {
					if (!now)
						checkDuplicate.run();
				}));
		intensityDropdown.focusedProperty().addListener((obs, was, now) -> {
			if (!now)
				checkDuplicate.run();
		});

		// ❌ Add delete button to remove this row from the VBox
		Button deleteButton = new Button("❌");
		deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
		deleteButton.setOnAction(e -> sunAzimuthRangesBox.getChildren().remove(rangeInput));

		// 📦 Compose the row
		rangeInput.getChildren().addAll(minField, maxField, pulsesField, intensityDropdown, durationField,
				intervalField, deleteButton);
		return rangeInput;
	}

	/**
	 * Adds a new empty Sun Azimuth range input row to the UI, if the configuration
	 * has already been fetched.
	 */
	@FXML
	private void addSunAzimuthRange() {
		// Prevent adding mappings until configurations are loaded
		if (!fetchConfigurationClicked) {
			showAlert("Action Blocked", "Please click 'Fetch Current' before adding new mappings.");
			return;
		}

		// Create and add a new input row for Sun Azimuth
		HBox newRange = createSunAzimuthInput("Sun");
		if (newRange != null) {
			sunAzimuthRangesBox.getChildren().add(newRange);
		}
	}
	
	/**
	 * Gathers all sun azimuth data from the UI into `sunRangeInputs` for saving. It
	 * updates active flags for existing entries and validates + stores new entries.
	 *
	 * @return true if all input rows are valid and data is ready to send; false
	 *         otherwise.
	 */
	@SuppressWarnings("unchecked")
	@FXML
	private boolean handleSaveSunData() {
		System.out.println("🔁 Updating visibility and saving new Sun Azimuth Ranges...");

		sunRangeInputs.clear(); // Reset the buffer used for backend submission

		// 1. 🔄 Update visibility status of previously parsed (existing) entries
		for (int i = 0; i < sunRangeInputsTest.size(); i++) {
			Node node = sunAzimuthRangesBox.getChildren().get(i);
			if (node instanceof HBox) {
				AzimuthRange range = sunRangeInputsTest.get(i);
				range.setActive(node.isVisible());
			}
		}

		// 2. ➕ Collect and validate newly added UI input rows
		for (int i = sunRangeInputsTest.size(); i < sunAzimuthRangesBox.getChildren().size(); i++) {
			Node node = sunAzimuthRangesBox.getChildren().get(i);
			if (node instanceof HBox rangeInput) {

				// Access each UI field from the HBox by index
				TextField minField = (TextField) rangeInput.getChildren().get(0);
				TextField maxField = (TextField) rangeInput.getChildren().get(1);
				TextField pulsesField = (TextField) rangeInput.getChildren().get(2);
				ComboBox<Integer> intensityDropdown = (ComboBox<Integer>) rangeInput.getChildren().get(3);
				TextField durationField = (TextField) rangeInput.getChildren().get(4);
				TextField intervalField = (TextField) rangeInput.getChildren().get(5);

				// Validate required fields
				if (minField.getText().isEmpty() || maxField.getText().isEmpty() || pulsesField.getText().isEmpty()
						|| durationField.getText().isEmpty() || intervalField.getText().isEmpty()
						|| intensityDropdown.getValue() == null) {
					showAlert("Validation Error", "Please fill in all fields before saving.");
					return false;
				}

				try {
					// Parse and create a new AzimuthRange object
					int minValue = Integer.parseInt(minField.getText());
					int maxValue = Integer.parseInt(maxField.getText());
					int pulses = Integer.parseInt(pulsesField.getText());
					int intensity = intensityDropdown.getValue();
					int duration = Integer.parseInt(durationField.getText());
					int interval = Integer.parseInt(intervalField.getText());

					AzimuthRange azimuthRange = new AzimuthRange();
					azimuthRange.setMinAzimuth(minValue);
					azimuthRange.setMaxAzimuth(maxValue);
					azimuthRange.setPulses(pulses);
					azimuthRange.setIntensity(intensity);
					azimuthRange.setDuration(duration);
					azimuthRange.setInterval(interval);
					azimuthRange.setActive(rangeInput.isVisible());

					// Add to both processing and persistent lists
					sunRangeInputs.add(azimuthRange);
					sunRangeInputsTest.add(azimuthRange);

				} catch (NumberFormatException e) {
					// Handle invalid input formats
					showAlert("Input Error", "All fields must contain valid numbers.");
					return false;
				}
			}
		}

		return true;
	}
	
	
	/**
	 * Creates a new row (HBox) for user input of a Moon Azimuth threshold. Includes
	 * fields for range and vibration configuration, duplicate prevention, and
	 * delete button.
	 *
	 * @param metricName label identifier (unused but kept for compatibility)
	 * @return a fully configured HBox with input fields and validation
	 */
	private HBox createMoonAzimuthInput(String metricName) {
		HBox rangeInput = new HBox(10); // UI row with spacing between fields

		// 📥 Create input fields with tooltips and constraints
		TextField minField = createLabeledField("MinAzimuth", 80, "Minimum azimuth angle (0° = North, 180° = South)");
		TextField maxField = createLabeledField("MaxAzimuth", 80, "Maximum azimuth angle (0° = North, 180° = South)");
		TextField pulsesField = createLabeledField("Pulses", 80, "Number of vibration pulses.");
		ComboBox<Integer> intensityDropdown = createLabeledComboBox("Intensity", 100,
				"Intensity level of vibration (1=Low, 5=High).");
		TextField durationField = createLabeledField("Duration (ms)", 100,
				"Duration of each vibration pulse in milliseconds.");
		TextField intervalField = createLabeledField("Interval (ms)", 100,
				"Time between each vibration pulse in milliseconds.");

		// 🚫 Duplicate check: Prevent user from adding the exact same config
		Runnable checkDuplicate = () -> {
			String min = minField.getText(), max = maxField.getText(), pulses = pulsesField.getText();
			String duration = durationField.getText(), interval = intervalField.getText();
			Integer intensity = intensityDropdown.getValue();

			if (min.isEmpty() || max.isEmpty() || pulses.isEmpty() || duration.isEmpty() || interval.isEmpty()
					|| intensity == null)
				return;

			boolean isDuplicate = moonAzimuthRangesBox.getChildren().stream()
					.filter(node -> node instanceof HBox && node != rangeInput && node.isVisible())
					.map(node -> (HBox) node).anyMatch(existing -> {
						try {
							return min.equals(((TextField) existing.getChildren().get(0)).getText())
									&& max.equals(((TextField) existing.getChildren().get(1)).getText())
									&& pulses.equals(((TextField) existing.getChildren().get(2)).getText())
									&& intensity.equals(((ComboBox<?>) existing.getChildren().get(3)).getValue())
									&& duration.equals(((TextField) existing.getChildren().get(4)).getText())
									&& interval.equals(((TextField) existing.getChildren().get(5)).getText());
						} catch (Exception e) {
							return false;
						}
					});

			if (isDuplicate) {
				showAlert("Duplicate Mapping", "This Moon Azimuth range already exists.");
				intervalField.clear(); // Prompt user to edit
			}
		};

		// 🎯 Attach duplicate check to all relevant input fields
		Stream.of(minField, maxField, pulsesField, durationField, intervalField)
				.forEach(field -> field.focusedProperty().addListener((obs, was, now) -> {
					if (!now)
						checkDuplicate.run();
				}));
		intensityDropdown.focusedProperty().addListener((obs, was, now) -> {
			if (!now)
				checkDuplicate.run();
		});

		// ❌ Add delete button to remove this row from the VBox
		Button deleteButton = new Button("❌");
		deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
		deleteButton.setOnAction(e -> moonAzimuthRangesBox.getChildren().remove(rangeInput));

		// 📦 Compose the row
		rangeInput.getChildren().addAll(minField, maxField, pulsesField, intensityDropdown, durationField,
				intervalField, deleteButton);
		return rangeInput;
	}
	
	@FXML
	private void addMoonAzimuthRange() {
		// Prevent adding mappings until configurations are loaded
		if (!fetchConfigurationClicked) {
			showAlert("Action Blocked", "Please click 'Fetch Current' before adding new mappings.");
			return;
		}

		// Create and add a new input row for Moon Azimuth
		HBox newRange = createMoonAzimuthInput("Moon");
		if (newRange != null) {
			moonAzimuthRangesBox.getChildren().add(newRange);
		}
	}
	
	/**
	 * Gathers all moon azimuth data from the UI into `moonRangeInputsTest` for saving. It
	 * updates active flags for existing entries and validates + stores new entries.
	 *
	 * @return true if all input rows are valid and data is ready to send; false
	 *         otherwise.
	 */
	@SuppressWarnings("unchecked")
	@FXML
	private boolean handleSaveMoonData() {
		System.out.println("🔁 Updating visibility and saving new Moon Azimuth Ranges...");


		// 1. 🔄 Update visibility status of previously parsed (existing) entries
		for (int i = 0; i < moonRangeInputsTest.size(); i++) {
			Node node = moonAzimuthRangesBox.getChildren().get(i);
			if (node instanceof HBox) {
				AzimuthRange range = moonRangeInputsTest.get(i);
				range.setActive(node.isVisible());
			}
		}

		// 2. ➕ Collect and validate newly added UI input rows
		for (int i = moonRangeInputsTest.size(); i < moonAzimuthRangesBox.getChildren().size(); i++) {
			Node node = moonAzimuthRangesBox.getChildren().get(i);
			if (node instanceof HBox rangeInput) {

				// Access each UI field from the HBox by index
				TextField minField = (TextField) rangeInput.getChildren().get(0);
				TextField maxField = (TextField) rangeInput.getChildren().get(1);
				TextField pulsesField = (TextField) rangeInput.getChildren().get(2);
				ComboBox<Integer> intensityDropdown = (ComboBox<Integer>) rangeInput.getChildren().get(3);
				TextField durationField = (TextField) rangeInput.getChildren().get(4);
				TextField intervalField = (TextField) rangeInput.getChildren().get(5);

				// Validate required fields
				if (minField.getText().isEmpty() || maxField.getText().isEmpty() || pulsesField.getText().isEmpty()
						|| durationField.getText().isEmpty() || intervalField.getText().isEmpty()
						|| intensityDropdown.getValue() == null) {
					showAlert("Validation Error", "Please fill in all fields before saving.");
					return false;
				}

				try {
					// Parse and create a new AzimuthRange object
					int minValue = Integer.parseInt(minField.getText());
					int maxValue = Integer.parseInt(maxField.getText());
					int pulses = Integer.parseInt(pulsesField.getText());
					int intensity = intensityDropdown.getValue();
					int duration = Integer.parseInt(durationField.getText());
					int interval = Integer.parseInt(intervalField.getText());

					AzimuthRange azimuthRange = new AzimuthRange();
					azimuthRange.setMinAzimuth(minValue);
					azimuthRange.setMaxAzimuth(maxValue);
					azimuthRange.setPulses(pulses);
					azimuthRange.setIntensity(intensity);
					azimuthRange.setDuration(duration);
					azimuthRange.setInterval(interval);
					azimuthRange.setActive(rangeInput.isVisible());

					// Add to both processing and persistent lists
					moonRangeInputsTest.add(azimuthRange);

				} catch (NumberFormatException e) {
					// Handle invalid input formats
					showAlert("Input Error", "All fields must contain valid numbers.");
					return false;
				}
			}
		}

		return true;
	}
	


	/**
	 * Adds a new empty row to the Heart Rate Mappings section in the UI.
	 * 
	 * Each row allows the user to configure a threshold mapping with: - Min/Max
	 * heart rate range - Vibration pulse configuration (pulses, intensity,
	 * duration, interval)
	 * 
	 * The method also includes a duplicate check to prevent identical mappings and
	 * a delete button to remove the added row.
	 */
	@FXML
	private void addHeartRateMappingInput() {
		// ⛔ Prevent adding rows until configurations have been fetched
		if (!fetchConfigurationClicked) {
			showAlert("Action Blocked", "Please click 'Fetch Current' before adding new mappings.");
			return;
		}

		// 📦 Create a new input row (HBox) for heart rate mapping
		HBox mappingInput = new HBox(10);

		// 🧾 Initialize all input fields with labels, tooltips, and widths
		TextField minField = createLabeledField("Min HR", 80, "Minimum heart rate value for this range");
		TextField maxField = createLabeledField("Max HR", 80, "Maximum heart rate value for this range");
		TextField pulsesField = createLabeledField("Pulses", 80, "Number of vibration pulses");
		ComboBox<Integer> intensityDropdown = createLabeledComboBox("Intensity", 100,
				"Intensity level of vibration (1=Low, 5=High)");
		TextField durationField = createLabeledField("Duration (ms)", 100,
				"Duration of each vibration pulse in milliseconds");
		TextField intervalField = createLabeledField("Interval (ms)", 100,
				"Time between each vibration pulse in milliseconds");

		// 🔍 Define duplicate check logic to prevent identical mappings from being
		// added
		Runnable checkDuplicate = () -> {
			String min = minField.getText(), max = maxField.getText(), pulses = pulsesField.getText();
			String duration = durationField.getText(), interval = intervalField.getText();
			Integer intensity = intensityDropdown.getValue();

			// Skip validation if any field is empty
			if (min.isEmpty() || max.isEmpty() || pulses.isEmpty() || duration.isEmpty() || interval.isEmpty()
					|| intensity == null)
				return;

			// Check if this mapping already exists in the VBox
			boolean isDuplicate = heartRateMappingsBox.getChildren().stream()
					.filter(node -> node instanceof HBox && node != mappingInput && node.isVisible())
					.map(node -> (HBox) node).anyMatch(existing -> {
						try {
							return min.equals(((TextField) existing.getChildren().get(0)).getText())
									&& max.equals(((TextField) existing.getChildren().get(1)).getText())
									&& pulses.equals(((TextField) existing.getChildren().get(2)).getText())
									&& intensity.equals(((ComboBox<?>) existing.getChildren().get(3)).getValue())
									&& duration.equals(((TextField) existing.getChildren().get(4)).getText())
									&& interval.equals(((TextField) existing.getChildren().get(5)).getText());
						} catch (Exception e) {
							return false;
						}
					});

			// ❗ If duplicate, notify user and clear one field to trigger editing
			if (isDuplicate) {
				showAlert("Duplicate Mapping", "This heart rate mapping already exists.");
				intervalField.clear();
			}
		};

		// 🔁 Attach duplicate check to each field (on focus loss)
		Stream.of(minField, maxField, pulsesField, durationField, intervalField)
				.forEach(field -> field.focusedProperty().addListener((obs, was, now) -> {
					if (!now)
						checkDuplicate.run();
				}));
		intensityDropdown.focusedProperty().addListener((obs, was, now) -> {
			if (!now)
				checkDuplicate.run();
		});

		// ❌ Add delete button to allow removal of this input row
		Button deleteButton = new Button("❌");
		deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
		deleteButton.setOnAction(e -> heartRateMappingsBox.getChildren().remove(mappingInput));

		// 🧱 Assemble the full row and add it to the VBox
		mappingInput.getChildren().addAll(minField, maxField, pulsesField, intensityDropdown, durationField,
				intervalField, deleteButton);
		heartRateMappingsBox.getChildren().add(mappingInput);
	}

	/**
	 * Collects all heart rate mapping inputs from the UI and stores them for
	 * backend submission.
	 * 
	 * This method: 1. Updates the `active` status of previously loaded mappings. 2.
	 * Validates and saves any newly added mappings into the internal buffer.
	 *
	 * @return true if all entries are valid and ready to send; false if validation
	 *         fails.
	 */
	@SuppressWarnings("unchecked")
	@FXML
	private Boolean saveHeartRateMappings() {
		System.out.println("🔁 Updating visibility and saving new Heart Rate Mappings...");

		// Reset the buffer used for processing and sending
		heartRateMappings.clear();

		// Debug: Print all current mappings before changes
		for (HeartRateRange.HeartRateThresholdMapping mapping : heartRateMappingsTest) {
			System.out.println(mapping);
		}

		try {
			// ----------------------------------------------------------
			// Step 1️⃣: Update visibility of previously parsed entries
			// ----------------------------------------------------------
			for (int i = 0; i < heartRateMappingsTest.size(); i++) {
				Node node = heartRateMappingsBox.getChildren().get(i);
				if (node instanceof HBox) {
					HeartRateRange.HeartRateThresholdMapping existing = heartRateMappingsTest.get(i);
					existing.setActive(node.isVisible()); // Reflect deleted rows as inactive
					System.out.println(node.isVisible() + " ASD");
					System.out.println(existing);
				}
			}

			// ---------------------------------------------------------------
			// Step 2️⃣: Process any newly added UI rows (not yet in test list)
			// ---------------------------------------------------------------
			for (int i = heartRateMappingsTest.size(); i < heartRateMappingsBox.getChildren().size(); i++) {
				Node node = heartRateMappingsBox.getChildren().get(i);
				if (node instanceof HBox input) {

					// Extract fields from the row
					TextField minField = (TextField) input.getChildren().get(0);
					TextField maxField = (TextField) input.getChildren().get(1);
					TextField pulsesField = (TextField) input.getChildren().get(2);
					ComboBox<Integer> intensityDropdown = (ComboBox<Integer>) input.getChildren().get(3);
					TextField durationField = (TextField) input.getChildren().get(4);
					TextField intervalField = (TextField) input.getChildren().get(5);

					// Validate required input fields
					if (minField.getText().isEmpty() || maxField.getText().isEmpty()
							|| intensityDropdown.getValue() == null || pulsesField.getText().isEmpty()
							|| durationField.getText().isEmpty() || intervalField.getText().isEmpty()) {
						showAlert("Validation Error", "Please fill in all fields before saving.");
						return false;
					}

					// Parse and map values
					int min = Integer.parseInt(minField.getText());
					int max = Integer.parseInt(maxField.getText());
					int pulses = Integer.parseInt(pulsesField.getText());
					int intensity = intensityDropdown.getValue();
					int duration = Integer.parseInt(durationField.getText());
					int interval = Integer.parseInt(intervalField.getText());

					// Create and store a new mapping object
					HeartRateRange.HeartRateThresholdMapping mapping = new HeartRateRange.HeartRateThresholdMapping(min,
							max, intensity, pulses, duration, interval);
					mapping.setActive(input.isVisible());

					// Add to both the submission buffer and persistent list
					heartRateMappings.add(mapping);
					heartRateMappingsTest.add(mapping);
				}
			}

		} catch (NumberFormatException e) {
			// Handle parsing errors gracefully
			showAlert("Input Error", "All fields must be valid numbers.");
			return false;
		} catch (Exception e) {
			// Log and notify for any other unexpected exceptions
			e.printStackTrace();
			showAlert("Error", "An error occurred while saving: " + e.getMessage());
			return false;
		}

		// ✅ All rows successfully validated and saved
		return true;
	}

	/**
	 * Fetches the current configuration from Node-RED and updates the UI components
	 * with the retrieved values. This version omits success messages and is
	 * typically used internally after saving configurations to refresh the
	 * interface silently.
	 */
	public void fetchCurrentConfigurationsWithoutMessage() {
		try {
			// Fetch JSON data from Node-RED's configuration endpoint
			JsonNode rootNode = getJsonFromNodeRed("http://127.0.0.1:1880/current-configurations");

			// Debug: Print the entire response structure
			System.out.println(rootNode);

			// Parse and populate Sun Azimuth and Heart Rate mappings into their respective
			// UI containers
			parseSunAzimuthRanges(rootNode.path("sunAzimuthRanges"));
			parseMoonAzimuthRanges(rootNode.path("moonAzimuthRanges"));
			parseHeartRateMappings(rootNode.path("heartRateMappings"));

			// Debug: Print just the heart rate section for inspection
			System.out.println(rootNode.path("heartRateMappings"));

			// Mark that configurations have been successfully loaded
			fetchConfigurationClicked = true;

		} catch (Exception e) {
			// Handle any errors that occurred during fetch or parsing
			e.printStackTrace();
			showAlert("Error", "An error occurred while fetching configurations: " + e.getMessage());
		}
	}

	/**
	 * Fetches the current configuration from Node-RED and loads it into the UI.
	 * 
	 * This version displays a success alert to the user and is intended for manual
	 * triggering (e.g., via a "Fetch Current" button).
	 */
	public void fetchCurrentConfigurations() {
		try {
			// Fetch configuration JSON from the Node-RED backend
			JsonNode rootNode = getJsonFromNodeRed("http://127.0.0.1:1880/current-configurations");

			// Debug: Log full response from Node-RED
			System.out.println(rootNode);

			// Populate UI components with data from the response
			parseSunAzimuthRanges(rootNode.path("sunAzimuthRanges"));
			parseMoonAzimuthRanges(rootNode.path("moonAzimuthRanges"));
			parseHeartRateMappings(rootNode.path("heartRateMappings"));

			// Debug: Print each parsed heart rate mapping object
			for (HeartRateRange.HeartRateThresholdMapping p : heartRateMappingsTest) {
				System.out.println(p + " HERE v2");
			}

			// Debug: Print heart rate section of the response
			System.out.println(rootNode.path("heartRateMappings"));

			// Notify the user that configurations were loaded successfully
			showAlert("Current Configurations", "Successfully fetched and loaded configurations.");

			// Flag that the configuration has been fetched at least once
			fetchConfigurationClicked = true;

		} catch (Exception e) {
			// Handle any network or parsing errors
			e.printStackTrace();
			showAlert("Error", "An error occurred while fetching configurations: " + e.getMessage());
		}
	}

	/**
	 * Parses the list of Sun Azimuth threshold configurations from a JSON array and
	 * renders each one as an editable row in the UI.
	 * 
	 * Each parsed object is stored in the internal `sunRangeInputsTest` list and
	 * linked to a corresponding HBox row with editable input fields.
	 *
	 * @param sunAzimuthRanges the JSON array node containing sun azimuth range
	 *                         objects
	 */
	private void parseSunAzimuthRanges(JsonNode sunAzimuthRanges) {
		// Clear any previously loaded data and UI rows
		sunRangeInputsTest.clear();
		sunAzimuthRangesBox.getChildren().clear();

		// Check that the provided node is a valid array
		if (sunAzimuthRanges != null && sunAzimuthRanges.isArray()) {
			for (JsonNode range : sunAzimuthRanges) {
				// Create and populate a new AzimuthRange object from JSON
				AzimuthRange model = new AzimuthRange();
				model.setId(range.path("id").asInt());
				model.setMinAzimuth(range.path("minvalue").asInt());
				model.setMaxAzimuth(range.path("maxvalue").asInt());
				model.setPulses(range.path("pulses").asInt());
				model.setIntensity(range.path("intensity").asInt());
				model.setDuration(range.path("duration").asInt());
				model.setInterval(range.path("interval").asInt());
				model.setActive(true);

				// Add the model to the internal test list
				sunRangeInputsTest.add(model);

				// Create a new UI row (HBox) for editing this configuration
				HBox row = new HBox(10);
				row.setId("sun_range_" + model.getId());

				// Bind fields directly to the model using helper functions
				row.getChildren().addAll(bindIntegerField(model.getMinAzimuth(), 80, model::setMinAzimuth),
						bindIntegerField(model.getMaxAzimuth(), 80, model::setMaxAzimuth),
						bindIntegerField(model.getPulses(), 80, model::setPulses),
						bindComboBox(model.getIntensity(), model::setIntensity),
						bindIntegerField(model.getDuration(), 100, model::setDuration),
						bindIntegerField(model.getInterval(), 100, model::setInterval),
						createDeleteButton(row, () -> model.setActive(false)) // Logical (not visual) delete
				);

				// Add the row to the VBox container in the UI
				sunAzimuthRangesBox.getChildren().add(row);
			}
		}
	}

	
	/**
	 * Parses the list of Moon Azimuth threshold configurations from a JSON array and
	 * renders each one as an editable row in the UI.
	 * 
	 * Each parsed object is stored in the internal `moonRangeInputsTest` list and
	 * linked to a corresponding HBox row with editable input fields.
	 *
	 * @param moonAzimuthRanges the JSON array node containing sun azimuth range
	 *                         objects
	 */
	private void parseMoonAzimuthRanges(JsonNode moonAzimuthRanges) {
		// Clear any previously loaded data and UI rows
		moonRangeInputsTest.clear();
		this.moonAzimuthRangesBox.getChildren().clear();

		// Check that the provided node is a valid array
		if (moonAzimuthRanges != null && moonAzimuthRanges.isArray()) {
			for (JsonNode range : moonAzimuthRanges) {
				// Create and populate a new AzimuthRange object from JSON
				AzimuthRange model = new AzimuthRange();
				model.setId(range.path("id").asInt());
				model.setMinAzimuth(range.path("minvalue").asInt());
				model.setMaxAzimuth(range.path("maxvalue").asInt());
				model.setPulses(range.path("pulses").asInt());
				model.setIntensity(range.path("intensity").asInt());
				model.setDuration(range.path("duration").asInt());
				model.setInterval(range.path("interval").asInt());
				model.setActive(true);

				// Add the model to the internal test list
				moonRangeInputsTest.add(model);

				// Create a new UI row (HBox) for editing this configuration
				HBox row = new HBox(10);
				row.setId("moon_range_" + model.getId());

				// Bind fields directly to the model using helper functions
				row.getChildren().addAll(bindIntegerField(model.getMinAzimuth(), 80, model::setMinAzimuth),
						bindIntegerField(model.getMaxAzimuth(), 80, model::setMaxAzimuth),
						bindIntegerField(model.getPulses(), 80, model::setPulses),
						bindComboBox(model.getIntensity(), model::setIntensity),
						bindIntegerField(model.getDuration(), 100, model::setDuration),
						bindIntegerField(model.getInterval(), 100, model::setInterval),
						createDeleteButton(row, () -> model.setActive(false)) // Logical (not visual) delete
				);

				// Add the row to the VBox container in the UI
				this.moonAzimuthRangesBox.getChildren().add(row);
			}
		}
	}
	
	
	/**
	 * Parses the list of Heart Rate threshold mappings from a JSON array and
	 * displays them in the UI as editable rows.
	 * 
	 * Each mapping is represented as a `HeartRateThresholdMapping` object, stored
	 * in `heartRateMappingsTest`, and linked to a corresponding UI row (HBox) for
	 * user interaction and potential modification.
	 *
	 * @param heartRateMappingsNode the JSON array containing heart rate threshold
	 *                              mappings
	 */
	private void parseHeartRateMappings(JsonNode heartRateMappingsNode) {
		// Clear existing in-memory mappings and UI rows
		heartRateMappingsTest.clear();
		heartRateMappingsBox.getChildren().clear();

		// Ensure the input node is a valid array
		if (heartRateMappingsNode != null && heartRateMappingsNode.isArray()) {
			for (JsonNode mapping : heartRateMappingsNode) {
				// Create a new model object from JSON values
				HeartRateThresholdMapping model = new HeartRateThresholdMapping(mapping.path("minvalue").asInt(),
						mapping.path("maxvalue").asInt(), mapping.path("intensity").asInt(),
						mapping.path("pulses").asInt(), mapping.path("duration").asInt(),
						mapping.path("interval").asInt());
				model.setId(mapping.path("id").asInt());
				model.setActive(true);

				// Add to the persistent test list for later use
				heartRateMappingsTest.add(model);

				// Create an editable UI row (HBox) for the mapping
				HBox row = new HBox(10);
				row.setId("hr_range_" + model.getId());

				// Add input fields bound to the model's values and setters
				row.getChildren().addAll(bindIntegerField(model.getMin(), 80, model::setMin),
						bindIntegerField(model.getMax(), 80, model::setMax),
						bindIntegerField(model.getPulses(), 80, model::setPulses),
						bindComboBox(model.getIntensity(), model::setIntensity),
						bindIntegerField(model.getDuration(), 100, model::setDuration),
						bindIntegerField(model.getInterval(), 100, model::setInterval),
						createDeleteButton(row, () -> model.setActive(false)) // Soft delete
				);

				// Add the completed row to the UI
				heartRateMappingsBox.getChildren().add(row);
			}
		}
	}

	/**
	 * Sends a given Java object as a JSON payload to a specified Node-RED HTTP
	 * endpoint via POST.
	 * 
	 * The method serializes the object to JSON, sets appropriate headers, and
	 * writes the content to the HTTP request body. It prints debug information and
	 * shows an error alert on failure.
	 *
	 * @param data           The object to serialize and send as JSON
	 * @param endpointUrl    The full URL of the Node-RED endpoint to POST to
	 * @param successMessage A success message to display or log (currently printed
	 *                       but unused)
	 * @return returns true if it was sent successfully else false.
	 */
	private boolean postJsonToNodeRed(Object data, String endpointUrl, String successMessage) {
	    HttpURLConnection connection = null;

	    try {
	        // 🔁 Convert Java object to JSON string
	        String jsonInputString = new ObjectMapper().writeValueAsString(data);
	        System.out.println("Sending JSON to Node-RED (" + endpointUrl + "): " + jsonInputString);

	        // 🌐 Prepare HTTP POST connection to the specified URL
	        URL url = new URL(endpointUrl);
	        connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("Content-Type", "application/json");
	        connection.setDoOutput(true); // Enable writing to the request body

	        // 📤 Write the JSON string to the request output stream
	        try (OutputStream os = connection.getOutputStream()) {
	            byte[] input = jsonInputString.getBytes("utf-8");
	            os.write(input, 0, input.length);
	        }

	        // ✅ Read the HTTP response code to determine success
	        int responseCode = connection.getResponseCode();
	        if (responseCode == HttpURLConnection.HTTP_OK) {
	            System.out.println("mappings sent successfully");
	            return true;
	        } else {
	            System.out.println("Failed to send data. Response code: " + responseCode);
	            return false;
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        showAlert("Error", "An error occurred: " + e.getMessage());
	        return false;
	    } finally {
	        // 🔚 Ensure connection is always disconnected
	        if (connection != null) {
	            connection.disconnect();
	        }
	    }
	}


	/**
	 * Applies an input filter to the provided TextField to restrict input to valid
	 * integers.
	 * 
	 * This includes both positive and negative numbers (e.g., "42", "-10"). Any
	 * non-numeric or partially invalid input is blocked in real-time.
	 *
	 * @param textField the TextField to enforce integer-only input on
	 */
	private void enforceIntegerInput(TextField textField) {
		// Define a filter that only allows digits and an optional leading minus sign
		UnaryOperator<TextFormatter.Change> integerFilter = change -> {
			String newText = change.getControlNewText();
			if (newText.matches("-?\\d*")) { // Regex: optional '-' followed by digits
				return change; // Accept change
			}
			return null; // Reject change (invalid input)
		};

		// Apply the filter using a TextFormatter
		textField.setTextFormatter(new TextFormatter<>(integerFilter));
	}

	/**
	 * Sends a GET request to the specified Node-RED endpoint and parses the
	 * response as a JSON node.
	 *
	 * @param urlString the full URL of the Node-RED endpoint to query
	 * @return a JsonNode representing the parsed JSON response
	 * @throws IOException if the connection fails or the server returns a non-200
	 *                     response
	 */
	private JsonNode getJsonFromNodeRed(String urlString) throws IOException {
		@SuppressWarnings("deprecation") // URL usage is appropriate here
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		// Configure the request
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");

		// Check for a successful response
		int responseCode = connection.getResponseCode();
		if (responseCode != 200) {
			throw new IOException("Failed with HTTP code: " + responseCode);
		}

		// Read and parse the JSON response
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {

			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line.trim()); // Concatenate lines, removing extra whitespace
			}

			// Convert raw JSON string to JsonNode using Jackson
			return new ObjectMapper().readTree(response.toString());

		} finally {
			// Always disconnect to free up the HTTP connection
			connection.disconnect();
		}
	}

	/**
	 * Creates a TextField initialized with a given integer value, enforces integer
	 * input, sets width, and binds its changes to the provided setter.
	 *
	 * @param initialValue initial integer value to display
	 * @param width        preferred width of the field
	 * @param setter       function to update the corresponding model property
	 * @return the configured TextField
	 */
	private TextField bindIntegerField(int initialValue, double width, java.util.function.IntConsumer setter) {
		TextField field = new TextField(String.valueOf(initialValue));
		field.setPrefWidth(width);
		enforceIntegerInput(field);

		// Update model when user types a new value
		field.setOnKeyReleased(e -> {
			try {
				setter.accept(field.getText().isEmpty() ? 0 : Integer.parseInt(field.getText()));
			} catch (NumberFormatException ignored) {
				// Input is invalid, ignore without crashing
			}
		});

		return field;
	}

	/**
	 * Creates a ComboBox for selecting integer intensity values (1–5), pre-selects
	 * an initial value, sets width, and binds changes to the provided setter.
	 *
	 * @param initialValue the value to select initially
	 * @param setter       function to update the corresponding model property
	 * @return the configured ComboBox
	 */
	private ComboBox<Integer> bindComboBox(int initialValue, java.util.function.IntConsumer setter) {
		ComboBox<Integer> combo = new ComboBox<>();
		combo.getItems().addAll(1, 2, 3, 4, 5);
		combo.setValue(initialValue);
		combo.setPrefWidth(100);

		// Update model when selection changes
		combo.valueProperty().addListener((obs, oldVal, newVal) -> {
			if (newVal != null)
				setter.accept(newVal);
		});

		return combo;
	}

	/**
	 * Creates a styled delete button for a given container. When clicked, the
	 * button hides the container (soft delete) and runs a custom callback.
	 *
	 * @param container the HBox or parent to hide
	 * @param onDelete  a callback to update model state or perform cleanup
	 * @return the configured delete Button
	 */
	private Button createDeleteButton(HBox container, Runnable onDelete) {
		Button deleteButton = new Button("❌");
		deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

		// On click: hide the row visually and trigger model cleanup
		deleteButton.setOnAction(e -> {
			container.setVisible(false);
			container.setManaged(false); // Exclude from layout
			onDelete.run();
		});

		return deleteButton;
	}

	/**
	 * Creates a generic integer-only TextField with a prompt, width, and tooltip.
	 * Does not bind to a model directly — intended for dynamic row input.
	 *
	 * @param prompt      the placeholder text for the field
	 * @param width       the preferred width of the field
	 * @param tooltipText the tooltip explaining the field's purpose
	 * @return the configured TextField
	 */
	private TextField createLabeledField(String prompt, double width, String tooltipText) {
		TextField field = new TextField();
		field.setPromptText(prompt);
		field.setPrefWidth(width);
		enforceIntegerInput(field); // Restrict input to valid integers
		Tooltip.install(field, new Tooltip(tooltipText));
		return field;
	}

	/**
	 * Creates a labeled ComboBox for selecting vibration intensity (1–5), with
	 * prompt text, fixed width, and tooltip.
	 *
	 * @param prompt      the placeholder text to show when no value is selected
	 * @param width       the preferred width of the ComboBox
	 * @param tooltipText the tooltip that explains the purpose of this field
	 * @return the configured ComboBox with intensity options
	 */
	private ComboBox<Integer> createLabeledComboBox(String prompt, double width, String tooltipText) {
		ComboBox<Integer> combo = new ComboBox<>();
		combo.getItems().addAll(1, 2, 3, 4, 5);
		combo.setPromptText(prompt);
		combo.setPrefWidth(width);
		Tooltip.install(combo, new Tooltip(tooltipText));
		return combo;
	}

	/**
	 * Populates the monitoring type ComboBox with available monitoring options.
	 * This method is typically called during UI initialization.
	 */
	private void populateMonitorTypes() {
		monitoringComboBox.getItems().addAll("HeartRate", "SunAzimuth","MoonAzimuth");
	}

	/**
	 * Displays an informational alert dialog with the given title and message.
	 *
	 * @param title   the title of the alert dialog
	 * @param message the content/message to display inside the dialog
	 */
	private void showAlert(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setContentText(message);
		alert.showAndWait();
	}

	/**
	 * Navigates the user to the visualization page (visPage.fxml). Triggered by a
	 * UI button event.
	 *
	 * @param event the action event that triggered the navigation
	 */
	@FXML
	private void goToVisPage(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/view/visPage.fxml"));
			Parent visPageRoot = loader.load();

			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			Scene scene = new Scene(visPageRoot);
			stage.setScene(scene);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace(); // Log the error for debugging
		}
	}

}
