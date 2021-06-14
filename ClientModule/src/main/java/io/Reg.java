package io;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Reg extends Application {
    public static Stage stage;

    public static Stage getStage() {
        return stage;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        Parent parent = FXMLLoader.load(getClass().getResource("reg.fxml"));
        primaryStage.setScene(new Scene(parent));
        primaryStage.show();
    }
}
