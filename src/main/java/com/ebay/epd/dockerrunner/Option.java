package com.ebay.epd.dockerrunner;

public class Option<T> {
    private final T value;

    private Option(T value) {
        this.value = value;
    }

    public static <T> Option<T> None() {
        return new Option<T>(null);
    }

    public static <T> Option<T> Some(T value) {
        return new Option<T>(value);
    }

    public void doIfPresent(OptionalCommand<T> fn) {
        if (value != null) {
            fn.apply(value);
        }
    }

    public interface OptionalCommand<T> {
        void apply(T arg);
    }
}
