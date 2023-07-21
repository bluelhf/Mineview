package blue.lhf.mineview.model;

import com.fasterxml.jackson.annotation.*;

public enum VersionType {
    OLD_BETA("old_beta"),
    OLD_ALPHA("old_alpha"),
    RELEASE("release"),
    SNAPSHOT("snapshot"),
    @JsonEnumDefaultValue
    UNKNOWN(null);

    private final String key;

    VersionType(final String key) {
        this.key = key;
    }

    @JsonValue
    public String getKey() {
        return key;
    }

    public static VersionType fromKey(final String key) {

        for (final VersionType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }

        return UNKNOWN;
    }
}
