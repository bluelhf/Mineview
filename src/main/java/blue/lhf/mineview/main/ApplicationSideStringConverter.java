package blue.lhf.mineview.main;

import blue.lhf.mineview.model.ApplicationSide;

public class ApplicationSideStringConverter extends javafx.util.StringConverter<blue.lhf.mineview.model.ApplicationSide> {
    @Override
    public String toString(final ApplicationSide applicationSide) {
        return toProperCase(applicationSide.name());
    }

    private String toProperCase(final String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    @Override
    public ApplicationSide fromString(final String name) {
        return ApplicationSide.valueOf(name.toUpperCase());
    }
}
