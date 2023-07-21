package blue.lhf.mineview.progress;

import blue.lhf.mineview.Mineview;
import blue.lhf.mineview.main.Options;
import blue.lhf.mineview.model.Deobfuscation;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProgressController {

    @FXML private ListView<String> progressLog;
    @FXML private ProgressBar progressBar;
    @FXML private ImageView loader;

    public void start(final Options options) {
        final Deobfuscation deobfuscation = new Deobfuscation(options.getApplicationSide(),
                options.getSelectedVersion(), options.getOutputDirectory());

        deobfuscation.onProgress((progress, total) -> progressBar.setProgress((double) progress / total));

        this.progressLog.setItems(deobfuscation.getLogger().last(1000));
        this.progressLog.getItems().addListener((ListChangeListener<String>) unused ->
                progressLog.scrollTo(progressLog.getItems().size() - 1));

        final Thread deobfuscator = new Thread(() -> {
            try {
                deobfuscation.run();
                loader.setImage(new Image("assets/success.gif"));
            } catch (final Exception e) {
                Mineview.catchCatastrophicFailure(e, "deobfuscating the archive");
            }
        });


        deobfuscator.setName("Deobfuscator Thread");
        deobfuscator.start();
    }
}