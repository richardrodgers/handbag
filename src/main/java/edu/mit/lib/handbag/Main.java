/**
 * Copyright 2014 MIT Libraries
 * Licensed under: http://www.apache.org/licenses/LICENSE-2.0
 */
package edu.mit.lib.handbag;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.controlsfx.dialog.Dialogs;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/handbag.fxml"));
        Parent root = (Parent)loader.load();
        Controller controller = (Controller)loader.getController();
        primaryStage.setTitle("HandBag");
        Scene scene = new Scene(root, 750, 420);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        Map<String, String> appProps = new HashMap<>();
        appProps.put("os", System.getProperty("os.name"));
        appProps.put("dispatcher", "http://mitlib-scads.appspot.com/workflows");
        Map<String, String> appParams = getParameters().getNamed();
        appProps.putAll(appParams);
        Optional<String> agentOpt = Optional.of("anon");
        if (appProps.containsKey("agent")) {
            agentOpt = Dialogs.create().owner(primaryStage).
                       title("Agent").message("Enter Agent ID").showTextInput();
        }
        controller.setAppProperties(appProps);
        controller.setAgent(agentOpt.orElse("anon"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
