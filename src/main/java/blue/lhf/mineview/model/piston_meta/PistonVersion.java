package blue.lhf.mineview.model.piston_meta;

import blue.lhf.mineview.model.VersionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PistonVersion(String id, VersionType type, URL url, Instant releaseTime)
    implements Comparable<PistonVersion> {

    public static final Instant MAPPINGS_CUTOFF = Instant.parse("2019-09-04T11:19:34Z");

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof final PistonVersion pv) {
            return pv.id.equals(id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo(@NotNull final PistonVersion o) {
        return releaseTime.compareTo(o.releaseTime);
    }

    public boolean hasMappings() {
        return !releaseTime.isBefore(MAPPINGS_CUTOFF) || id.equals("1.14.4");
    }

    public PistonVersionMeta fetch() throws IOException {
        return PistonVersionMeta.forVersion(this);
    }
}
