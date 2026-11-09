package dev.spod;

public final class MiningTorchesTest {
    public static void main(String[] args) {
        check(MiningTorches.covers(13, 14, 0), "Regular torches light thirteen unobstructed blocks away");
        check(!MiningTorches.covers(14, 14, 0), "Zero light does not prevent spawning");
        check(MiningTorches.covers(9, 10, 0), "Soul torches have shorter coverage");
        check(!MiningTorches.covers(10, 10, 0), "Do not use regular torch spacing for soul torches");
        check(MiningTorches.covers(6, 14, 7), "Custom dimension light limits affect coverage");
        check(!MiningTorches.covers(7, 14, 7), "Light equal to the spawn limit is insufficient");
        check(!MiningTorches.covers(0, 14, 15), "Torches cannot prevent spawns when the limit exceeds their brightness");
        System.out.println("Torch coverage checks passed: regular/soul torch ranges and dimension light thresholds.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
