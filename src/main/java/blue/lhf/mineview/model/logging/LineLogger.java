package blue.lhf.mineview.model.logging;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LineLogger {
    private final ObservableList<String> log = FXCollections.observableList(new ArrayList<>());

    public ObservableList<String> getLog() {
        return log;
    }

    public CompletableFuture<Integer> log(final String message) {
        if (!Platform.isFxApplicationThread()) {
            final CompletableFuture<Integer> future = new CompletableFuture<>();
            Platform.runLater(() -> {
                log.add(message);
                future.complete(log.size() - 1);
            });
            return future;
        }
        log.add(message);
        return CompletableFuture.completedFuture(log.size() - 1);
    }
    
    public void relog(final int index, final String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> relog(index, message));
            return;
        }

        log.set(index, message);
    }

    public void append(final int index, final String line) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> append(index, line));
            return;
        }
        log.set(index, log.get(index) + "\n" + line);
    }

    public ObservableList<String> last(final int count) {
        return new ObservableListBase<String>() {
            void init() {
                log.addListener((ListChangeListener<String>) c -> {
                    this.fireChange(new ListChangeListener.Change<>(this) {

                        @Override
                        public boolean next() {
                            return c.next();
                        }

                        @Override
                        public void reset() {
                            c.reset();
                        }

                        @Override
                        public int getFrom() {
                            return c.getFrom() - Math.min(count, c.getFrom());
                        }

                        @Override
                        public int getTo() {
                            return c.getTo() - Math.min(count, c.getTo());
                        }

                        @Override
                        public List<String> getRemoved() {
                            return (List<String>) c.getRemoved();
                        }

                        @Override
                        protected int[] getPermutation() {
                            if (c.wasPermutated()) {
                                final int[] permutation = new int[getTo() - getFrom()];
                                for (int i = 0; i < permutation.length; i++) {
                                    permutation[i] = c.getPermutation(i) - Math.min(count, log.size());
                                }
                                return permutation;
                            } else {
                                return new int[0];
                            }
                        }
                    });
                });
            }

            {
                init();
            }

            @Override
            public String get(final int index) {
                if (index < 0 || index >= count) {
                    throw new IndexOutOfBoundsException();
                }
                return log.get(log.size() - Math.min(count, log.size()) + index);
            }

            @Override
            public int size() {
                return Math.min(count, log.size());
            }
        };
    }
}
