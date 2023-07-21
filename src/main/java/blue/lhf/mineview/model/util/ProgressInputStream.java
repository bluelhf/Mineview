package blue.lhf.mineview.model.util;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class ProgressInputStream extends InputStream {

    private final InputStream backingStream;
    private final AtomicLong progress = new AtomicLong(0);
    private final List<Consumer<Long>> progressListeners = new ArrayList<>();
    private final long size;

    public ProgressInputStream(final InputStream input, final long size) {
        this.backingStream = input;
        this.size = size;
    }

    @Override
    public int read() throws IOException {
        this.addProgress(1);
        return this.backingStream.read();
    }

    @Override
    public int read(final byte @NotNull [] b, final int off, final int len) throws IOException {
        final int result = this.backingStream.read(b, off, len);
        this.addProgress(result);
        return result;
    }

    public ProgressInputStream onProgress(final Consumer<Long> incrementConsumer) {
        this.progressListeners.add(incrementConsumer);
        return this;
    }

    private void addProgress(final long increment) {
        this.progress.addAndGet(increment);
        this.progressListeners.forEach(c -> c.accept(increment));
    }

    public long getProgress() {
        return progress.get();
    }

    public long getSize() {
        return size;
    }

    public ProgressInputStream withTotal(final Consumer<Long> totalConsumer) {
        totalConsumer.accept(this.size);
        return this;
    }
}
