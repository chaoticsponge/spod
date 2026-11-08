package dev.spod;

public final class ReconnectScheduleTest {
    public static void main(String[] args) {
        ReconnectSchedule schedule = new ReconnectSchedule();
        long now = 1000;
        check(!schedule.takeAttempt(now), "No retry before a disconnect");
        for (int attempt = 1; attempt <= 20; attempt++) {
            long delay = Math.min(attempt, 5) * 60000L;
            schedule.disconnected(now);
            schedule.disconnected(now + 5000);
            check(schedule.remainingSeconds(now) == delay / 1000, "Duplicate screens must not extend the delay");
            check(!schedule.takeAttempt(now + delay - 1), "Must wait the full cooldown");
            check(schedule.remainingSeconds(now + delay - 1) == 1, "Display rounds up the final second");
            check(schedule.takeAttempt(now + delay), "Attempt must start at its deadline");
            check(!schedule.takeAttempt(now + delay), "An attempt must not start twice");
            now += delay + 12345;
        }
        schedule.disconnected(now);
        schedule.reset();
        check(!schedule.takeAttempt(now + 3600000), "Cancel/success must clear pending attempts");
        schedule.disconnected(now);
        check(schedule.remainingSeconds(now) == 60, "A new disconnect after success starts at one minute");
        check(schedule.takeAttempt(now + 3600000), "A delayed tick can start the overdue attempt");
        check(!schedule.takeAttempt(now + 3600000), "A delayed tick must not create catch-up bursts");
        System.out.println("Reconnect checks passed: 1/2/3/4/5-minute delays, indefinite cap, reset, cancellation and single attempts.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
