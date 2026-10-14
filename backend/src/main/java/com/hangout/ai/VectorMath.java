package com.hangout.ai;

public final class VectorMath {

    private VectorMath() {}

    /** Similarité cosinus entre deux vecteurs (0 si l'un est nul ou de taille différente). */
    public static double cosine(double[] a, double[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0.0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}