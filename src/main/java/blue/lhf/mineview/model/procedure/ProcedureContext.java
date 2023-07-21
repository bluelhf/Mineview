package blue.lhf.mineview.model.procedure;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class ProcedureContext {
    protected final List<Consumer<ProcedureContext>> progressListeners = new CopyOnWriteArrayList<>();

    protected final AtomicLong progress = new AtomicLong(0);
    protected final AtomicLong total = new AtomicLong(1);

    public void setProgress(final long progress) {
        this.progress.set(progress);
        this.notifyProgress();
    }

    public void addProgress(final long progress) {
        this.progress.addAndGet(progress);
        this.notifyProgress();
    }

    public void setTotal(final long total) {
        this.total.set(total);
        this.notifyProgress();
    }

    private void notifyProgress() {
        this.progressListeners.forEach(c -> c.accept(this));
    }

    public void setProgress(final long progress, final long total) {
        this.progress.set(progress);
        this.total.set(total);
        this.notifyProgress();
    }

    public void onProgress(final Consumer<ProcedureContext> incrementConsumer) {
        this.progressListeners.add(incrementConsumer);
    }

    public long getProgress() {
        return progress.get();
    }

    public long getTotal() {
        return total.get();
    }
}
