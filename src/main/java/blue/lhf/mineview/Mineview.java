package blue.lhf.mineview;

import blue.lhf.mineview.main.*;
import blue.lhf.mineview.progress.ProgressController;
import javafx.application.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.*;

import java.io.*;
import java.net.URL;

public class Mineview extends Application {
    private Stage stage;

    public static void catchCatastrophicFailure(final Throwable throwable, final String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> catchCatastrophicFailure(throwable, message));
            return;
        }
        throwable.printStackTrace();
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("A catastrophic failure occurred while " + message);

        final StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        final TextArea label = new TextArea("The exception stacktrace was:\n" + sw);
        label.setWrapText(false);
        alert.getDialogPane().setContent(label);
        alert.showAndWait();
        Platform.exit();
    }

    @Override
    public void start(final Stage stage) throws IOException {
        final URL main = getClass().getResource("/main.fxml");
        if (main == null) throw new AssertionError("main.fxml not found -- is the JAR corrupted?");

        final FXMLLoader loader = new FXMLLoader(main);
        final Parent parent = loader.load();
        final MainController controller = loader.getController();
        controller.setHost(this);
        this.stage = stage;

        stage.setTitle("Mineview");
        stage.setScene(new Scene(parent));
        stage.show();
    }

    public Window getStage() {
        return stage;
    }

    public void startProcess(final Options options) {
        final URL progress = getClass().getResource("/progress.fxml");
        try {
            if (progress == null) throw new AssertionError("progress.fxml not found -- is the JAR corrupted?");
            final FXMLLoader loader = new FXMLLoader(progress);
            final Parent parent = loader.load();
            final ProgressController controller = loader.getController();
            final double width = stage.getWidth(), height = stage.getHeight();
            stage.setScene(new Scene(parent));
            stage.setWidth(width);
            stage.setHeight(height);
            controller.start(options);
        } catch (final IOException e) {
            catchCatastrophicFailure(e, "loading the progress screen");
        }
    }
}
