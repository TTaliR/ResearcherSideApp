package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class JavaFXApp extends Application {

	@Override
	public void start(Stage primaryStage) {
		try {
			// Load FXML and Scene
			FXMLLoader fxmlLoader = new FXMLLoader(
					getClass().getResource("/com/example/demo/view/ResearcherInterface.fxml"));
			Scene scene = new Scene(fxmlLoader.load());

			// Set the application icon (must be inside resources folder)
			primaryStage.getIcons()
					.add(new Image(getClass().getResourceAsStream("/com/example/demo/images/app-icon.png")));

			primaryStage.setTitle("Smart Watch Haptic System");
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
