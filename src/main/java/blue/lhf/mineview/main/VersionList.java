package blue.lhf.mineview.main;

import blue.lhf.mineview.model.VersionType;
import blue.lhf.mineview.model.piston_meta.PistonVersion;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;

import java.util.*;

public class VersionList {
    private final FilteredList<PistonVersion> backingList;

    private boolean showingAllVersions = false;

    public VersionList(final NavigableSet<PistonVersion> set) {
        this.backingList = new FilteredList<>(FXCollections.observableList(new ArrayList<>(set.descendingSet())), this::shouldShow);
    }

    public boolean isShowingAllVersions() {
        return showingAllVersions;
    }

    public FilteredList<PistonVersion> getBackingList() {
        return backingList;
    }

    public void setShowingAllVersions(final boolean showingAllVersions) {
        this.showingAllVersions = showingAllVersions;
        this.backingList.setPredicate(this::shouldShow);
    }

    boolean shouldShow(final PistonVersion version) {
        if (showingAllVersions) return true;
        return version.type() == VersionType.RELEASE;
    }
}
