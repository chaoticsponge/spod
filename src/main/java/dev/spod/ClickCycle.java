package dev.spod;

/** A repeating hold/release schedule measured in Minecraft's 20 ticks per second. */
public record ClickCycle(int holdTicks, int intervalTicks) {
    public static final double MIN_SECONDS = 0.05;
    public static final double MAX_SECONDS = 86400;

    public ClickCycle {
        if (holdTicks < 1 || intervalTicks < 1 || holdTicks > 1728000 || intervalTicks > 1728000) {
            throw new IllegalArgumentException("Durations must be between one tick and one day");
        }
    }

    public static ClickCycle seconds(double hold, double interval) {
        return new ClickCycle(ticks(hold), ticks(interval));
    }

    private static int ticks(double seconds) {
        if (!Double.isFinite(seconds) || seconds < MIN_SECONDS || seconds > MAX_SECONDS) {
            throw new IllegalArgumentException("Seconds must be between 0.05 and 86400");
        }
        return (int) Math.ceil(seconds * 20);
    }

    public boolean isHolding(long elapsedTicks) {
        return elapsedTicks % (holdTicks + intervalTicks) < holdTicks;
    }
}
