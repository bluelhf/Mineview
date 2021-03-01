package io.github.bluelhf.mineview.ext.unmapper.util;

import java.util.ArrayList;
import java.util.function.BiConsumer;

public class Observable<T> extends ArrayList<BiConsumer<T, T>> {
    private T current;

    public Observable(T initial) {
        current = initial;
    }

    public void set(T newValue) {
        for (BiConsumer<T, T> listener : this) {
            listener.accept(current, newValue);
        }
        current = newValue;
    }

    public T get() {
        return current;
    }

    public void onChanged(BiConsumer<T, T> listener) {
        add(listener);
    }
}
