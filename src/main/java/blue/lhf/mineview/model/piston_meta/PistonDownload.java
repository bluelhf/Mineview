package blue.lhf.mineview.model.piston_meta;

import blue.lhf.mineview.model.util.VerifiedInputStream;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PistonDownload(String sha1, long size, URL url) {
    public VerifiedInputStream openStream() throws IOException, NoSuchAlgorithmException {
        return new VerifiedInputStream(url.openStream(), sha1, size);
    }
}
