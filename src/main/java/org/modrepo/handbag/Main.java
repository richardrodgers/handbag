/**
 * Copyright 2023 Richard Rodgers
 * SPDX-Licence-Identifier: Apache-2.0
 */
package org.modrepo.handbag;

import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        ResourceBundle textBundle = ResourceBundle.getBundle("UIText");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/handbag.fxml"), textBundle);
        Parent root = (Parent)loader.load();
        primaryStage.setTitle(textBundle.getString("appTitle"));
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
