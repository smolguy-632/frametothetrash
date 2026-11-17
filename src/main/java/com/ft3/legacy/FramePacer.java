package com.ft3.legacy;

/**
 * Decides, once per rendered frame, whether that frame is put on the screen or thrown away.
 * <p>
 * Everything here runs on the render thread, so there is no queue, no scheduler thread and no
 * locking. That is what keeps the "latest frame only" guarantee simple:
 * <ul>
 * <li>A frame is only ever presented <b>right after it finished rendering</b>, so what reaches the
 * screen is always the newest complete frame. An older frame can never be shown late, because
 * nothing is stored between frames.</li>
 * <li>Frames that are not presented are simply overwritten in the back buffer by the next one.</li>
 * <li>If the game stalls and several presentation slots are missed, they are dropped rather than
 * replayed as a burst. The schedule resynchronises to "now".</li>
 * <li>Of the frames around a presentation deadline, the one that finishes <em>closest</em> to the
 * deadline is the one chosen, so the image on screen is as fresh as possible when it appears.</li>
 * </ul>
 */
public final class FramePacer {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    /** Weight of the newest sample in the moving average of frame time. */
    private static final double SMOOTHING = 0.1D;

    /** Deadline (System.nanoTime) of the next presentation slot, 0 = not started. */
    private static long nextPresentNanos;
    /** System.nanoTime when the previous frame finished, 0 = no previous frame. */
    private static long lastFrameEndNanos;
    /** Moving average of the time between two finished frames, in nanoseconds. */
    private static double averageFrameNanos;

    private FramePacer() {}

    /**
     * Call once at the end of every rendered frame.
     *
     * @return {@code true} if the frame that just finished should be swapped to the screen,
     *         {@code false} if it should be discarded.
     */
    public static boolean shouldPresent() {
        if (!FT3Config.enabled) {
            reset();
            return true;
        }
        return shouldPresent(System.nanoTime(), FT3Config.updatesPerSecond);
    }

    /** Time-injected variant of {@link #shouldPresent()}, kept package-private so it can be tested. */
    static boolean shouldPresent(long now, int updatesPerSecond) {
        if (updatesPerSecond < 1) {
            updatesPerSecond = 1;
        }
        final long interval = NANOS_PER_SECOND / updatesPerSecond;

        if (lastFrameEndNanos != 0L) {
            // Clamp to one interval: a long stall must not skew the average and make the
            // frames right after it get presented early.
            final long frameNanos = Math.min(now - lastFrameEndNanos, interval);
            averageFrameNanos = averageFrameNanos == 0.0D
                    ? frameNanos
                    : averageFrameNanos + (frameNanos - averageFrameNanos) * SMOOTHING;
        }
        lastFrameEndNanos = now;

        if (nextPresentNanos == 0L) {
            // very first frame: show it immediately
            nextPresentNanos = now + interval;
            return true;
        }

        // If the following frame is expected to end further past the deadline than this one
        // ends before it, this frame is the closest one to the deadline: present it now.
        if (now + (long) (averageFrameNanos * 0.5D) >= nextPresentNanos) {
            nextPresentNanos += interval;
            if (nextPresentNanos <= now) {
                // We are behind by a full slot or more. Drop the missed slots instead of
                // presenting several frames back to back.
                nextPresentNanos = now + interval;
            }
            return true;
        }
        return false;
    }

    private static void reset() {
        nextPresentNanos = 0L;
        lastFrameEndNanos = 0L;
        averageFrameNanos = 0.0D;
    }
}
