package io.github.bluelhf.mineview;

import io.github.bluelhf.mineview.main.Controller;
import io.github.bluelhf.mineview.main.Model;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Mediator extends Application {
    public static Mediator INSTANCE;
    public final Executor NETWORK_POOL = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final HttpClient client = HttpClient.newBuilder()
            .executor(NETWORK_POOL)
            .build();

    private Controller controller;
    private Model model = new Model();
    private Stage stage;

    public Mediator() {
        INSTANCE = this;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    @Override
    public void start(Stage stage) throws IOException {
        Font.loadFont(getClass().getResourceAsStream("/fonts/aAntaraDistance.ttf"), 96);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        model = new Model();
        Parent root = loader.load();

        Scene scene = new Scene(root, root.prefWidth(-1), root.prefHeight(-1));
        stage.setTitle("MineView v0000");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image("/images/logo.png"));
        this.stage = stage;

        controller = loader.getController();

        model.init();
        controller.init();

        stage.show();
    }

    public Controller getController() {
        return controller;
    }

    public Model getModel() {
        return model;
    }

    public HttpClient getClient() {
        return client;
    }

    public Stage getStage() {
        return stage;
    }

}
