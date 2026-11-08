package dev.spod;

public final class ClickCycleTest {
    public static void main(String[] args) {
        ClickCycle equal = ClickCycle.seconds(1, 1);
        check(equal.isHolding(0) && equal.isHolding(19), "First second must hold");
        check(!equal.isHolding(20) && !equal.isHolding(39), "Second second must release");
        check(equal.isHolding(40), "Third second must begin the next hold");

        ClickCycle mending = ClickCycle.seconds(0.1, 1);
        check(mending.holdTicks() == 2 && mending.intervalTicks() == 20, "Mending must hold for 0.1s and release for 1s");
        check(mending.isHolding(0) && mending.isHolding(1), "Mending swing must last two ticks");
        check(!mending.isHolding(2) && !mending.isHolding(21) && mending.isHolding(22),
                "Mending must wait a full second before its next swing");

        ClickCycle unequal = ClickCycle.seconds(0.5, 2);
        for (int tick = 0; tick < 150; tick++) {
            check(unequal.isHolding(tick) == (tick < 10 || tick >= 50 && tick < 60 || tick >= 100 && tick < 110),
                    "Unequal durations must keep the release interval separate from the hold");
        }
        check(ClickCycle.seconds(0.051, 0.05).holdTicks() == 2, "Fractional seconds round up to full ticks");
        ClickCycle largest = ClickCycle.seconds(86400, 86400);
        check(!largest.isHolding(1728000) && largest.isHolding(3456000), "Long cycles must not overflow");
        for (double value : new double[]{0, -1, 0.049, 86401, Double.NaN, Double.POSITIVE_INFINITY}) {
            rejects(value, 1);
            rejects(1, value);
        }
        System.out.println("Click cycle checks passed: boundaries, repeats, unequal intervals, fractional ticks and invalid inputs.");
    }

    private static void rejects(double hold, double interval) {
        try {
            ClickCycle.seconds(hold, interval);
            throw new AssertionError("Invalid duration accepted");
        } catch (IllegalArgumentException expected) {
            // Invalid durations must never reach the running controller.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
