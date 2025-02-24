package com.logisticscraft.occlusionculling.util;

public final class MathUtilities {

    private MathUtilities() {
    }

    public static int floor(double d) {
        int i = (int) d;
        return d < (double) i ? i - 1 : i;
    }

}
