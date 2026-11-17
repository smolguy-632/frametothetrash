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
}
