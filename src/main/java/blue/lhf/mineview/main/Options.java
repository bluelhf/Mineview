package blue.lhf.mineview.main;

import blue.lhf.mineview.model.ApplicationSide;
import blue.lhf.mineview.model.piston_meta.PistonVersion;

import java.nio.file.Path;

public final class Options {
    private PistonVersion selectedVersion;
    private ApplicationSide applicationSide;
    private Path outputDirectory = Path.of(".");

    public void setSelectedVersion(final PistonVersion selectedVersion) {
        this.selectedVersion = selectedVersion;
    }

    public void setApplicationSide(final ApplicationSide applicationSide) {
        this.applicationSide = applicationSide;
    }

    public void setOutputDirectory(final Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public PistonVersion getSelectedVersion() {
        return selectedVersion;
    }

    public ApplicationSide getApplicationSide() {
        return applicationSide;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }
}
