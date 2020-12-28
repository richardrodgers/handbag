/**
 * Copyright 2014 MIT Libraries
 * SPDX-Licence-Identifier: Apache-2.0
 */
package edu.mit.lib.handbag;

import java.util.Map;
import java.util.Optional;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

//import org.controlsfx.dialog.Dialogs;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/handbag.fxml"));
        Parent root = (Parent)loader.load();
        Controller controller = (Controller)loader.getController();
        primaryStage.setTitle("HandBag");
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
        Map<String, String> appProps = getParameters().getNamed();
        Optional<String> agentOpt = Optional.of("anon");
        /*
        if (appProps.containsKey("agent")) {
            agentOpt = Dialogs.create().owner(primaryStage).
                       title("Agent").message("Enter Agent ID").showTextInput();
        }
        */
        controller.setAppProperties(appProps);
        controller.setAgent(agentOpt.orElse("anon"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
