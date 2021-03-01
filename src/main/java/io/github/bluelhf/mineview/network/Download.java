package io.github.bluelhf.mineview.network;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Download {
    public final Side side;
    public final Type type;
    public final String sha1;
    public final long size;
    public final URI url;

    private Download(Side side, Type type, String sha1, long size, URI uri) {
        this.side = side;
        this.type = type;
        this.sha1 = sha1;
        this.size = size;
        this.url = uri;
    }

    public void download(Path target) {
        downloadSync(target);
    }

    public CompletableFuture<Void> download(Path target, Executor executor) {
        return CompletableFuture.runAsync(() -> downloadSync(target), executor);
    }

    private void downloadSync(Path target) {
        try {
            Files.copy(url.toURL().openStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Download failed", e);
        }
    }

    public static CompletableFuture<ArrayList<Download>> fromURI(HttpClient client, String address) {

        URI uri = URI.create(address);

        HttpRequest request = HttpRequest.newBuilder(uri).build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    ArrayList<Download> downloads = new ArrayList<>();
                    JSONObject baseObject = new JSONObject(body);
                    JSONObject downloadsObject = baseObject.getJSONObject("downloads");
                    for (Object downloadsKey : downloadsObject.names()) {
                        JSONObject entry = downloadsObject.getJSONObject((String) downloadsKey);
                        Type type = Type.fromKey((String) downloadsKey);
                        Side side = Side.fromKey((String) downloadsKey);

                        String sha1 = entry.getString("sha1");
                        long size = entry.getLong("size");
                        URI url = URI.create(entry.getString("url"));

                        downloads.add(new Download(side, type, sha1, size, url));
                    }

                    return downloads;
                });
    }


    public long getSize() {
        return size;
    }

    public Side getSide() {
        return side;
    }

    public URI getUrl() {
        return url;
    }

    public enum Type {
        JAR,
        MAPPINGS;

        public static Type fromKey(String key) {
            if (key.endsWith("_mappings")) return Type.MAPPINGS;
            return Type.JAR;
        }


        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
