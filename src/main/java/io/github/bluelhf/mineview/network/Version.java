package io.github.bluelhf.mineview.network;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

public class Version {

    public ArrayList<Download> getDownloads() {
        return downloads;
    }

    public Instant getReleaseDate() {
        return releaseDate;
    }

    public String getId() {
        return id;
    }

    public static class Builder {
        private Type type;
        private String id;
        private Instant releaseDate;
        private ArrayList<Download> downloads = new ArrayList<>();

        public Builder(String id) {
            this.id = id;
        }

        public Builder withType(Type type) {
            this.type = type;
            return this;
        }

        public Builder withReleaseDate(Instant date) {
            this.releaseDate = date;
            return this;
        }

        public Builder withDownload(Download download) {
            downloads.add(download);
            return this;
        }

        public Builder withDownloads(Collection<Download> downloads) {
            this.downloads.addAll(downloads);
            return this;
        }

        public Version build() {
            if (downloads.size() < 4) {
                throw new IllegalStateException("Must specify at least four download URLs.");
            }
            if (type == null) {
                throw new IllegalStateException("Must specify version type.");
            }
            if (id == null) {
                throw new IllegalStateException("Must specify ID.");
            }
            if (releaseDate == null) {
                throw new IllegalStateException("Must specify release date.");
            }

            Version version = new Version();
            version.type = type;
            version.id = id;
            version.releaseDate = releaseDate;
            version.downloads = downloads;
            return version;
        }
    }

    private Type type;
    private String id;
    private Instant releaseDate;
    private ArrayList<Download> downloads = new ArrayList<>();

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return id;
    }

    public enum Type {
        RELEASE("release"),
        SNAPSHOT("snapshot");

        private final String text;
        Type(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public static Type fromKey(String key) {
            for (Type type : values()) {
                if (type.text.equals(key)) {
                    return type;
                }
            }

            throw new IllegalArgumentException("No type exists for text '" + key + "'");
        }
    }
}
