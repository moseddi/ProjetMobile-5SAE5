package com.hangout.util;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Newest-first ordering for every list the app shows. MongoDB ObjectIds start
 * with their creation timestamp, so sorting ids in descending order puts the most
 * recently added record first, including legacy records that have no createdAt.
 */
public final class Newest {
    private Newest() {}

    public static <T> Comparator<T> first(Function<T, String> id) {
        return Comparator.comparing(id, Comparator.nullsLast(Comparator.<String>reverseOrder()));
    }
}
