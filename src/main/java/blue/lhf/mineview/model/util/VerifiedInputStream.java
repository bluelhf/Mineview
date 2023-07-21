package blue.lhf.mineview.model.util;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.security.*;

public class VerifiedInputStream extends InputStream {
    private final InputStream inner;

    private long bytesRead = 0L;
    private final @Nullable Long size;

    private record HashVerifier(String sha1, MessageDigest digest) {}

    @Nullable
    private final HashVerifier verifier;

    public VerifiedInputStream(final InputStream inner) {
        this(inner, (Long) null);
    }

    public VerifiedInputStream(final InputStream inner, final @Nullable Long size) {
        this.inner = inner;
        this.verifier = null;
        this.size = size;
    }

    public VerifiedInputStream(final InputStream inner, final String sha1) throws NoSuchAlgorithmException {
        this(inner, sha1, null);
    }

    public VerifiedInputStream(final InputStream inner, final String sha1, final @Nullable Number size) throws NoSuchAlgorithmException {
        this.inner = inner;
        this.size = size != null ? size.longValue() : null;
        this.verifier = new HashVerifier(sha1, MessageDigest.getInstance("SHA-1"));
    }

    @Override
    public int read() throws IOException {
        final int result = inner.read();
        if (result != -1) {
            ++this.bytesRead;
            updateVerifier((byte) result);
        }
        return result;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final byte[] aux = new byte[len];
        final int read = inner.read(aux, 0, len);
        if (read != -1) {
            this.bytesRead += read;
            updateVerifier(aux, read);
            System.arraycopy(aux, 0, b, off, read);
        }
        return read;
    }

    private void updateVerifier(final byte result) {
        if (verifier != null) verifier.digest.update(result);
    }

    private void updateVerifier(final byte[] bytes, final int read) {
        if (verifier != null) verifier.digest.update(bytes, 0, read);
    }

    @Override
    public void close() throws IOException {
        inner.close();
        if (verifier != null) {
            final String actualSha1 = bytesToHex(verifier.digest.digest());
            if (!actualSha1.equals(verifier.sha1)) throw new IncorrectHashException(actualSha1, verifier.sha1);
        }
        if (size != null && bytesRead != size) throw new IncorrectSizeException(bytesRead, size);
    }

    private String bytesToHex(final byte[] digest) {
        final StringBuilder builder = new StringBuilder();
        for (final byte b : digest) {
            builder.append(String.format("%02x", b));
        }

        return builder.toString();
    }

    static class VerificationException extends IOException {
        public VerificationException(final String message) {
            super(message);
        }
    }

    static class IncorrectHashException extends VerificationException {
        public IncorrectHashException(final String actual, final String expected) {
            super("Incorrect hash: expected " + expected + ", got " + actual);
        }
    }

    static class IncorrectSizeException extends VerificationException {
        public IncorrectSizeException(final long actual, final long expected) {
            super("Incorrect size: expected " + expected + ", got " + actual);
        }
    }
}
