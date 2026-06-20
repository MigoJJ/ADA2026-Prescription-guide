package com.t2drx;

import com.t2drx.controller.InputController;
import com.t2drx.controller.ResultController;
import com.t2drx.model.PatientData;
import com.t2drx.model.Recommendation;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private Stage primaryStage;
    private static App instance;

    public App() {
        instance = this;
    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        stage.setTitle("ADA 2026 Type 2 Diabetes Prescription Advisor");
        
        // Minimize window size constraints for standard displays
        stage.setMinWidth(960);
        stage.setMinHeight(700);

        showInputView();
        stage.show();
    }

    public void showInputView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/t2drx/view/InputView.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showResultView(PatientData patientData, Recommendation recommendation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/t2drx/view/ResultView.fxml"));
            Parent root = loader.load();

            ResultController controller = loader.getController();
            controller.setResults(patientData, recommendation);

            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
