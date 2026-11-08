package dev.spod;

/** One pending attempt at a time, using monotonic milliseconds rather than game ticks. */
public final class ReconnectSchedule {
    private int delayMinutes;
    private long deadline;
    private boolean waiting;

    public void disconnected(long now) {
        if (!waiting) {
            delayMinutes = Math.min(delayMinutes + 1, 5);
            deadline = now + delayMinutes * 60000L;
            waiting = true;
        }
    }

    public boolean takeAttempt(long now) {
        if (!waiting || now < deadline) {
            return false;
        }
        waiting = false;
        return true;
    }

    public long remainingSeconds(long now) {
        return waiting ? Math.max(0, (deadline - now + 999) / 1000) : 0;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public void reset() {
        delayMinutes = 0;
        waiting = false;
        deadline = 0;
    }
}
