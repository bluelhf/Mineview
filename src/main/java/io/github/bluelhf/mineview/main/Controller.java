package io.github.bluelhf.mineview.main;

import io.github.bluelhf.mineview.Mediator;
import io.github.bluelhf.mineview.ext.unmapper.util.Observable;
import io.github.bluelhf.mineview.network.Download;
import io.github.bluelhf.mineview.network.Side;
import io.github.bluelhf.mineview.network.Version;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;


public class Controller {
    public ComboBox<Version> versionSelector;
    @FXML
    public CheckBox snapshotCheckbox;

    @FXML
    public ImageView versionLoader;

    @FXML
    public ComboBox<Side> sideSelector;

    @FXML
    public Button directorySelector;

    @FXML
    public Button buildButton;

    @FXML
    public GridPane loader;

    @FXML
    public GridPane content;

    @FXML
    public ListView<String> loaderLog;

    @FXML
    public GridPane done;

    private CompletableFuture<Void> switchToLoader() {
        FadeTransition toLoader = new FadeTransition(Duration.millis(1000), loader);
        toLoader.setFromValue(loader.getOpacity());
        toLoader.setToValue(1);

        FadeTransition fromContent = new FadeTransition(Duration.millis(1000), content);
        fromContent.setFromValue(content.getOpacity());
        fromContent.setToValue(0);

        return CompletableFuture.runAsync(() -> {
            fromContent.playFromStart();
            toLoader.playFromStart();
            LockSupport.parkNanos((long) (1E+9));
        });
    }

    private CompletableFuture<Void> switchToDone() {
        FadeTransition toDone = new FadeTransition(Duration.millis(200), done);
        toDone.setFromValue(done.getOpacity());
        toDone.setToValue(1);

        FadeTransition fromLoader = new FadeTransition(Duration.millis(200), loader);
        fromLoader.setFromValue(loader.getOpacity());
        fromLoader.setToValue(0);

        return CompletableFuture.runAsync(() -> {
            toDone.playFromStart();
            fromLoader.playFromStart();
            LockSupport.parkNanos((long) (1E+9));
        });
    }

    private CompletableFuture<Void> hideVersionLoader() {
        FadeTransition transition = new FadeTransition(Duration.millis(500), versionLoader);
        transition.setFromValue(1);
        transition.setToValue(0);
        return CompletableFuture.runAsync(() -> {
            transition.play();
            LockSupport.parkNanos((long) (transition.getTotalDuration().toMillis() * 1E+6));
        });

    }


    public void init() {
        populateSideSelector();
        initVersionLoader();
        populateVersionSelector();
        initVersionSelector();
    }

    private void initVersionLoader() {
        versionLoader.setVisible(true);
        Mediator.INSTANCE.getModel().hasVersionsProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue) {
                hideVersionLoader().thenRun(() -> versionLoader.setVisible(false));
            } else {
                versionLoader.setVisible(true);
            }
        }));
    }

    private void initVersionSelector() {
        versionSelector.getItems().addListener((ListChangeListener<Version>) c -> {
            boolean added = false;
            while (c.next()) {
                if (c.wasAdded()) {
                    added = true;
                    break;
                }
            }

            if (added && versionSelector.isShowing()) {
                Platform.runLater(versionSelector::hide);
                Platform.runLater(versionSelector::show);
            }
        });
    }

    @FXML
    public void openDirectorySelector() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose an output directory");
        chooser.setInitialDirectory(Mediator.INSTANCE.getModel().getOutputDirectory());
        File outputDir = chooser.showDialog(Mediator.INSTANCE.getStage());
        if (outputDir != null) {
            directorySelector.setText(outputDir.getName());
            Mediator.INSTANCE.getModel().setOutputDirectory(outputDir);
        }
    }

    @FXML
    public boolean checkInputs() {
        if (sideSelector.getSelectionModel().isEmpty()) return false;
        if (versionSelector.getSelectionModel().isEmpty()) return false;
        buildButton.setDisable(false);
        return true;
    }

    @FXML
    public void populateSideSelector() {
        sideSelector.setItems(FXCollections.observableArrayList(Side.values()));
    }

    @FXML
    public void populateVersionSelector() {
        AtomicBoolean wasVisible = new AtomicBoolean(versionLoader.isVisible());

        versionLoader.setVisible(true);
        ChangeListener<Boolean> changeListener = (observable, oldVal, newVal) -> {
            wasVisible.set(newVal);
        };
        versionLoader.visibleProperty().addListener(changeListener);
        versionSelector.itemsProperty().setValue(Mediator.INSTANCE.getModel().getVersions(snapshotCheckbox.isSelected()));

        versionLoader.visibleProperty().removeListener(changeListener);
        versionLoader.setVisible(wasVisible.get());
    }

    private Controller log(String s) {
        Platform.runLater(() -> {
            loaderLog.getItems().add(s);
            loaderLog.scrollTo(loaderLog.getItems().size() - 1);
        });
        return this;
    }

    @FXML
    public void build() {
        if (checkInputs()) {
            content.setDisable(true);
            switchToLoader().thenRun(() -> {
                Observable<Model.State> observable = Mediator.INSTANCE.getModel().runWith(versionSelector.getValue(), sideSelector.getValue());
                observable.onChanged((oldState, newState) -> {
                    switch (newState.getType()) {
                        case DOWNLOADING:
                            for (Download d : newState.getDownloadFutures().keySet()) {
                                log("Downloading " + d.url)
                                        .log("\tType: " + d.type)
                                        .log("\tSide: " + d.side)
                                        .log("\tHash: " + d.sha1)
                                        .log("\tSize: " + d.size + " bytes");
                            }
                            break;
                        case UNMAPPING:
                            newState.getUnmappingState().onChanged((oldMapperState, newMapperState) -> {
                                switch (newMapperState) {
                                    case LOADING:
                                        log("Loading JAR...");
                                        break;
                                    case REMAPPING:
                                        log("Deobfuscating JAR...");
                                        break;
                                    case SAVING:
                                        log("Saving output JAR...");
                                        break;
                                }
                            });
                        case DONE:
                            log("Done!");
                            switchToDone();
                            break;
                    }
                });
            });
        }
    }
}
