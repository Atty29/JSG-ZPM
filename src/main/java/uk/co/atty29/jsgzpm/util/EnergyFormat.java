package uk.co.atty29.jsgzpm.util;

import java.util.Locale;

public final class EnergyFormat {
    private static final long KILO = 1_000L;
    private static final long MEGA = 1_000_000L;
    private static final long GIGA = 1_000_000_000L;
    private static final long TERA = 1_000_000_000_000L;

    private EnergyFormat() {
    }

    public static String format(long value) {
        long safe = Math.max(0L, value);

        if (safe >= TERA) {
            return formatScaled(safe, TERA, "T");
        }
        if (safe >= GIGA) {
            return formatScaled(safe, GIGA, "G");
        }
        if (safe >= MEGA) {
            return formatScaled(safe, MEGA, "M");
        }
        if (safe >= KILO) {
            return formatScaled(safe, KILO, "k");
        }
        return Long.toString(safe);
    }

    private static String formatScaled(long value, long divisor, String suffix) {
        double scaled = (double) value / (double) divisor;
        if (Math.abs(scaled - Math.rint(scaled)) < 0.000_000_1D) {
            return String.format(Locale.ROOT, "%.0f%s", scaled, suffix);
        }
        return String.format(Locale.ROOT, "%.1f%s", scaled, suffix);
    }
}
