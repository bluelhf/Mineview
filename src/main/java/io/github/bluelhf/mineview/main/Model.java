package io.github.bluelhf.mineview.main;

import io.github.bluelhf.mineview.Mediator;
import io.github.bluelhf.mineview.MineView;
import io.github.bluelhf.mineview.ext.unmapper.Unmapper;
import io.github.bluelhf.mineview.ext.unmapper.util.Observable;
import io.github.bluelhf.mineview.network.Download;
import io.github.bluelhf.mineview.network.Side;
import io.github.bluelhf.mineview.network.Version;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

public class Model {
    private ObservableList<Version> allVersions;
    private File outputDirectory = Paths.get(MineView.class.getProtectionDomain().getPermissions()
            .elements().nextElement().getName()).getParent().toFile();
    private final SimpleBooleanProperty hasVersionsProperty = new SimpleBooleanProperty(false);
    private Observable<State> state = new Observable<State>(State.idle());
    public void init() {
        outputDirectory = Paths.get(MineView.class.getProtectionDomain().getPermissions()
                .elements().nextElement().getName()).getParent().toFile();
        state.set(State.idle());
        if (!hasVersionsProperty.get()) allVersions = fetchVersions();
    }

    public Observable<State> runWith(Version version, Side side) {
        CompletableFuture.runAsync(() -> {
            LockSupport.parkNanos((long) 1E+9);
            HashMap<Download, CompletableFuture<Void>> futures = new HashMap<>();

            Path mappings = outputDirectory.toPath().resolve("mappings.txt.tmp");
            Path jar = outputDirectory.toPath().resolve(side.name().toLowerCase(Locale.ROOT) + ".jar.tmp");
            for (Download download : version.getDownloads()) {
                if (download.side != side) continue;
                switch (download.type) {
                    case MAPPINGS:
                        futures.put(download, download.download(mappings, Mediator.INSTANCE.NETWORK_POOL));
                        break;
                    case JAR:
                        futures.put(download, download.download(jar, Mediator.INSTANCE.NETWORK_POOL));
                        break;
                }
            }
            state.set(State.downloading(futures));
            CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new)).thenRun(() -> {
                Observable<Unmapper.State> observable = new Unmapper.Builder()
                        .withInput(jar.toFile())
                        .withMappings(mappings.toFile())
                        .withOutput(new File(outputDirectory, side.name().toLowerCase(Locale.ROOT) + "-" + version + "-remapped.jar"))
                        .removeLocals()
                        .run();
                observable.onChanged((oldMapperState, newMapperState) -> {
                    if (newMapperState == Unmapper.State.DONE) {
                        try {
                            Files.deleteIfExists(mappings);
                            Files.deleteIfExists(jar);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        state.set(State.done());
                    }
                });
                state.set(State.unmapping(observable));
            });
        });

        return state;
    }

    private ObservableList<Version> fetchVersions() {
        HttpRequest manifestRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://launchermeta.mojang.com/mc/game/version_manifest.json"))
                .build();

        ObservableList<Version> versions = FXCollections.observableArrayList();
        Mediator.INSTANCE.getClient().sendAsync(manifestRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept((response) -> {
                    try {
                        String body = response.body();
                        JSONObject object = new JSONObject(body);
                        for (Object obj : object.getJSONArray("versions")) {
                            JSONObject entry = (JSONObject) obj;

                            String id = entry.getString("id");
                            Version.Type type = Version.Type.fromKey(entry.getString("type"));
                            Instant releaseDate = Instant.parse(entry.getString("releaseTime").split("\\+")[0] + ".00Z");

                            ArrayList<Download> downloads = Download.fromURI(Mediator.INSTANCE.getClient(), entry.getString("url")).get();
                            if (downloads.size() < 4) return;
                            Version.Builder builder = new Version.Builder(id)
                                    .withType(type)
                                    .withReleaseDate(releaseDate)
                                    .withDownloads(downloads);

                            Platform.runLater(() -> versions.add(builder.build()));
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }).thenRun(() -> hasVersionsProperty.set(true));
        return versions;
    }

    public SimpleBooleanProperty hasVersionsProperty() {
        return hasVersionsProperty;
    }

    public FilteredList<Version> getVersions(boolean showSnapshots) {
        return allVersions.filtered((version) -> version.getType() == Version.Type.RELEASE || showSnapshots);
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public Observable<State> getState() {
        return state;
    }

    public static abstract class State {

        private static State ofType(Type type) {
            return new State() {
                @Override
                public Type getType() {
                    return type;
                }
            };
        }

        public static State downloading(HashMap<Download, CompletableFuture<Void>> downloadFutures) {
            return new State() {
                @Override
                public Type getType() {
                    return Type.DOWNLOADING;
                }

                @Override
                public boolean hasDownloadFutures() {
                    return true;
                }

                @Override
                public HashMap<Download, CompletableFuture<Void>> getDownloadFutures() {
                    return downloadFutures;
                }
            };
        }

        public static State idle() {
            return ofType(Type.IDLE);
        }

        public static State unmapping(Observable<Unmapper.State> state) {
            return new State() {
                @Override
                public Type getType() {
                    return Type.UNMAPPING;
                }

                @Override
                public boolean hasUnmappingState() {
                    return true;
                }

                @Override
                public Observable<Unmapper.State> getUnmappingState() {
                    return state;
                }
            };
        }

        public static State done() {
            return State.ofType(Type.DONE);
        }

        public abstract Type getType();

        public boolean hasUnmappingState() {
            return false;
        }

        public Observable<Unmapper.State> getUnmappingState() {
            return null;
        }

        public boolean hasDownloadFutures() {
            return false;
        }

        public HashMap<Download, CompletableFuture<Void>> getDownloadFutures() {
            return null;
        }

        public enum Type {
            IDLE,
            DOWNLOADING,
            UNMAPPING,
            DONE
        }
    }
}
