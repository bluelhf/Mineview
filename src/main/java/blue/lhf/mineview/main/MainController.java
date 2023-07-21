package blue.lhf.mineview.main;

import blue.lhf.mineview.Mineview;
import blue.lhf.mineview.model.ApplicationSide;
import blue.lhf.mineview.model.piston_meta.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.*;

public class MainController {
    private final Options options = new Options();
    private VersionList versionList;
    private Mineview host;

    public void setHost(final Mineview host) {
        this.host = host;
    }

    @FXML private ComboBox<PistonVersion> versionPicker;
    @FXML private CheckBox showAllVersions;

    @FXML private ComboBox<ApplicationSide> sidePicker;
    @FXML private Button directoryChooser;

    @FXML
    public void initialize() {
        final PistonMeta meta;
        try {
            meta = PistonMeta.fetch();
        } catch (final IOException e) {
            Mineview.catchCatastrophicFailure(e, "fetching the version list");
            return;
        }

        this.versionList = new VersionList(meta.versions());
        versionPicker.setItems(this.versionList.getBackingList());
        versionPicker.setConverter(new PistonVersionStringConverter(meta.versions()));
        versionPicker.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            options.setSelectedVersion(newValue);
        });
        versionPicker.getSelectionModel().selectFirst();

        sidePicker.getItems().addAll(ApplicationSide.values());
        sidePicker.setConverter(new ApplicationSideStringConverter());
        sidePicker.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            options.setApplicationSide(newValue);
        });
        sidePicker.getSelectionModel().selectFirst();
    }

    @FXML
    private void allVersionsChanged() {
        final SelectionModel<PistonVersion> selectionModel = versionPicker.getSelectionModel();

        final PistonVersion selectedVersion = selectionModel.getSelectedItem();
        this.versionList.setShowingAllVersions(showAllVersions.isSelected());
        if (selectedVersion != null && this.versionList.shouldShow(selectedVersion)) {
            selectionModel.select(selectedVersion);
        } else selectionModel.selectFirst();
    }

    @FXML
    private void directoryChooserClicked() {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select output directory");
        final File file = chooser.showDialog(host.getStage());
        if (file != null) {
            options.setOutputDirectory(file.toPath());
            directoryChooser.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void startPressed() {
        this.host.startProcess(options);
    }
}
