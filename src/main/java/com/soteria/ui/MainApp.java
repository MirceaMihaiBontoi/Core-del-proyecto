package com.soteria.ui;

import com.soteria.core.model.UserData;
import com.soteria.ui.controller.LoginController;
import com.soteria.ui.controller.ChatController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX application entry point for SoterIA.
 * Manages navigation between login and chat screens.
 */
public class MainApp extends Application {

    private Stage primaryStage;
    private Scene loginScene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
        // Load login screen
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
        Parent loginRoot = loginLoader.load();
        loginScene = new Scene(loginRoot, 700, 800);
        loginScene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        
        LoginController loginController = loginLoader.getController();
        loginController.setMainApp(this);
        
        // Configure stage
        primaryStage.setTitle("SoterIA - Emergency Management System");
        primaryStage.setScene(loginScene);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(700);
        primaryStage.show();
        
        Platform.runLater(() -> {
            primaryStage.sizeToScene();
            primaryStage.centerOnScreen();
        });
    }
    
    /**
     * Switches to the chat screen after successful login.
     */
    public void showChatScreen(UserData userData) throws Exception {
        FXMLLoader chatLoader = new FXMLLoader(getClass().getResource("/fxml/chat-view.fxml"));
        Parent chatRoot = chatLoader.load();
        
        ChatController chatController = chatLoader.getController();
        chatController.setUserData(userData);
        
        Scene chatScene = new Scene(chatRoot, 800, 700);
        chatScene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        
        primaryStage.setScene(chatScene);
        primaryStage.setTitle("SoterIA - Emergency Chat - " + userData.fullName());
    }
    
    /**
     * Returns to the login screen.
     */
    public void showLoginScreen() {
        primaryStage.setScene(loginScene);
        primaryStage.setTitle("SoterIA - Emergency Management System");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
