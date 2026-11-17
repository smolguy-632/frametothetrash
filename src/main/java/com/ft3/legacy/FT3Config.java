package com.ft3.legacy;

import net.minecraftforge.common.config.Config;

/**
 * Stored in {@code config/frametothetrash.cfg} and editable in-game from the Mods menu.
 * Forge fills the static fields automatically and {@link FrameToTheTrash} re-syncs them when
 * the config screen is saved, so changes apply immediately without a restart.
 */
@Config(modid = Tags.MODID, name = Tags.MODID)
public class FT3Config {

    @Config.Name("enabled")
    @Config.Comment("Master switch. When false, every rendered frame is shown, exactly like vanilla.")
    public static boolean enabled = true;

    @Config.Name("updatesPerSecond")
    @Config.Comment({
            "How many times per second a finished frame is actually put on the screen.",
            "The game keeps rendering as fast as it can; frames in between are thrown away and only",
            "the newest finished frame is ever displayed.",
            "Set this to your monitor's refresh rate (or a little above). Turn VSync OFF for best results."
    })
    @Config.RangeInt(min = 1, max = 1000)
    public static int updatesPerSecond = 60;

    @Config.Name("skipUnseenFrames")
    @Config.Comment({
            "Display decoupling: frames that will not be shown are not rendered at all.",
            "The game loop keeps ticking and polling input at full speed, but the world is drawn",
            "only when a frame is about to be put on the screen, and that frame is shown immediately",
            "after it finishes, so what you see is always the most recent possible image.",
            "In-game F3 FPS counts loop iterations and will go into the millions; that is normal.",
            "false = every loop iteration still renders a frame that is then thrown away (old behaviour)."
    })
    public static boolean skipUnseenFrames = true;
}
