package com.ft3.legacy;

/**
 * Decides, once per rendered frame, whether that frame is put on the screen or thrown away, and
 * in decoupled mode also which game-loop iterations render at all.
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
 * <li>Of the frames around each deadline, the one that finishes <em>closest</em> to the deadline is
 * the one chosen, so the image on screen is as fresh as possible when it appears.</li>
 * </ul>
 * <p>
 * In decoupled mode ({@code skipUnseenFrames}) the loop still ticks and polls input every
 * iteration, but the world is only drawn on an iteration whose frame will actually be presented.
 * The frame drawn that way is the newest thing the game can show: it is built from the input and
 * the tick state of the iteration that ends right at the presentation deadline, and it goes on the
 * screen the moment it is finished.
 */
public final class FramePacer {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    /** Weight of the newest sample in the moving average of frame time (legacy pacing only). */
    private static final double SMOOTHING = 0.1D;

    /** Deadline (System.nanoTime) of the next presentation slot, 0 = not started. */
    private static long nextPresentNanos;
    /** System.nanoTime when the previous frame finished, 0 = no previous frame. */
    private static long lastFrameEndNanos;
    /** Moving average of the time between two finished frames, in nanoseconds. */
    private static double averageFrameNanos;

    /** True when the current loop iteration is rendering a frame that will be presented. */
    private static boolean renderIteration;
    /** Presentation interval (ns) the current iteration's render decision was made with. */
    private static long pendingIntervalNanos;
    /** Cached decoupled flag, so toggling the config resets the schedule cleanly. */
    private static boolean decoupledMode;

    private FramePacer() {}

    /**
     * Render gate. Called once per game-loop iteration, right before the world-render block.
     *
     * @return {@code true} if this iteration must render a frame, {@code false} if the world render
     *         (and the framebuffer blit) can be skipped entirely because no frame will be presented.
     */
    public static boolean shouldRenderFrame(boolean screenOpen) {
        if (!FT3Config.enabled) {
            renderIteration = false;
            return true;
        }
        final boolean decoupled = FT3Config.skipUnseenFrames;
        if (decoupled != decoupledMode) {
            decoupledMode = decoupled;
            reset();
        }
        if (!decoupled) {
            // Legacy behaviour: every iteration renders; pacing happens at the present gate only.
            renderIteration = false;
            return true;
        }
        if (screenOpen) {
            // Open GUI screens: with divisor 1 (or in any odd config state) render and show every
            // iteration like before, so the UI stays maximally responsive. Otherwise pace screens
            // at updatesPerSecond / divisor, which saves a lot of GPU while menus are open; clicks
            // and typing stay responsive because input is polled every iteration regardless.
            int divisor = FT3Config.guiRateDivisor;
            if (divisor <= 1) {
                renderIteration = true;
                pendingIntervalNanos = intervalNanos(FT3Config.updatesPerSecond);
                return true;
            }
            return shouldRenderFrameDecoupled(System.nanoTime(), FT3Config.updatesPerSecond / divisor);
        }
        return shouldRenderFrameDecoupled(System.nanoTime(), FT3Config.updatesPerSecond);
    }

    private static long intervalNanos(int updatesPerSecond) {
        return NANOS_PER_SECOND / Math.max(1, updatesPerSecond);
    }

    /** Time-injected variant of the decoupled render decision, kept package-private for testing. */
    static boolean shouldRenderFrameDecoupled(long now, int updatesPerSecond) {
        final long interval = intervalNanos(updatesPerSecond);
        pendingIntervalNanos = interval;

        if (nextPresentNanos == 0L) {
            // very first frame: render and show it immediately
            nextPresentNanos = now + interval;
            renderIteration = true;
            return true;
        }

        // Render the first iteration that can still land near the deadline. Anything earlier is
        // skipped: it would only be overwritten by a newer frame before it is ever shown.
        if (now + interval / 2L >= nextPresentNanos) {
            renderIteration = true;
            return true;
        }
        renderIteration = false;
        return false;
    }

    /** Whether the current loop iteration is rendering a frame that will be presented. */
    public static boolean isRenderIteration() {
        return renderIteration;
    }

    /**
     * Present gate. Called once per loop iteration where {@code updateDisplay()} runs, i.e. on
     * <em>every</em> iteration (rendered or not), and also from vanilla's own loading screen
     * renderer. In decoupled mode it presents the frames the render gate marked; in legacy mode it
     * paces on its own as before.
     *
     * @param vanillaSkippedRender {@code true} when vanilla (or a mod) skipped the world render
     *                             this iteration via {@code skipRenderWorld} (loading screens).
     *                             The render gate never ran in that case, so the frame that was
     *                             drawn (if any) is always shown and the schedule is left alone
     *                             so pacing resumes normally once loading finishes.
     * @return {@code true} if the frame that just finished should be swapped to the screen,
     *         {@code false} if it should be discarded.
     */
    public static boolean shouldPresent(boolean vanillaSkippedRender) {
        if (!FT3Config.enabled) {
            reset();
            renderIteration = false;
            return true;
        }
        if (decoupledMode) {
            if (vanillaSkippedRender) {
                return true;
            }
            final boolean present = renderIteration;
            renderIteration = false;
            if (present) {
                final long now = System.nanoTime();
                lastFrameEndNanos = now;
                // Advance by the interval the render decision used, so paced GUI screens stay on
                // their own (slower) schedule while open and the world resyncs when they close.
                long interval = pendingIntervalNanos;
                if (interval <= 0L) {
                    interval = intervalNanos(FT3Config.updatesPerSecond);
                }
                nextPresentNanos = now + interval;
            }
            return present;
        }
        return shouldPresentLegacy(System.nanoTime(), FT3Config.updatesPerSecond);
    }

    /** Time-injected variant of the legacy present decision, kept package-private for testing. */
    static boolean shouldPresentLegacy(long now, int updatesPerSecond) {
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
        pendingIntervalNanos = 0L;
    }
}
