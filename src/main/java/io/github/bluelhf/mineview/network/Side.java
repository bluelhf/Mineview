package io.github.bluelhf.mineview.network;

import java.util.Locale;

public enum Side {
    CLIENT,
    SERVER;

    public static Side fromKey(String key) {
        if (key.startsWith("client")) return Side.CLIENT;
        if (key.startsWith("server")) return Side.SERVER;
        throw new IllegalArgumentException("No side exists for key '" + key + "'");
    }

    @Override
    public String toString() {
        return this.name().substring(0, 1).toUpperCase(Locale.ROOT) + this.name().substring(1).toLowerCase(Locale.ROOT);
    }
}
