package com.minkang.ultimate.pvpmm.util;

public class Elo {

    public static double expectedScore(double ra, double rb) {
        return 1.0 / (1.0 + Math.pow(10.0, (rb - ra) / 400.0));
    }

    public static int kFactor(int base, boolean solo, boolean underdog) {
        int k = base;
        if (solo) k += 4; // small nudge for solo
        if (underdog) k += 8;
        return k;
    }

    public static int clamp(int rating) {
        if (rating < 0) return 0;
        if (rating > 3000) return 3000;
        return rating;
    }
}
