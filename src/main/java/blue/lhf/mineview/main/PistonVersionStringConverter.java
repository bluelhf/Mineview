package blue.lhf.mineview.main;

import blue.lhf.mineview.model.piston_meta.PistonVersion;
import javafx.util.StringConverter;

import java.util.*;

public class PistonVersionStringConverter extends StringConverter<PistonVersion> {
    final Map<String, PistonVersion> idMap = new HashMap<>();

    public PistonVersionStringConverter(final Set<PistonVersion> versions) {
        for (final PistonVersion version : versions) {
            idMap.put(version.id(), version);
        }
    }

    @Override
    public String toString(final PistonVersion version) {
        if (version == null) return null;
        return version.id();
    }

    @Override
    public PistonVersion fromString(final String text) {
        if (text == null) return null;
        return idMap.get(text);
    }
}
