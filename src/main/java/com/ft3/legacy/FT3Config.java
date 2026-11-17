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

    @Config.Name("guiRateDivisor")
    @Config.Comment({
            "How much slower open GUI screens (menus, inventory, chat, config) are rendered and",
            "shown compared to updatesPerSecond. Only used while skipUnseenFrames is on.",
            "1 = screens render every loop iteration like before (uncapped).",
            "2 = screens update at half the updatesPerSecond rate (e.g. 30 when that is 60).",
            "3 = a third of the rate, and so on. Clicks and typing always stay responsive; only",
            "the picture updates slower, which saves a lot of GPU while you are in menus."
    })
    @Config.RangeInt(min = 1, max = 20)
    public static int guiRateDivisor = 2;

    @Config.Name("hudRateDivisor")
    @Config.Comment({
            "Exordium-style HUD caching: the in-game HUD (hotbar, crosshair, chat, potion icons,",
            "boss bars) is re-rendered into its own framebuffer at updatesPerSecond / hudRateDivisor",
            "and that cached image is stamped over every rendered frame in between, so the HUD does",
            "not have to be drawn at full frame rate.",
            "1 = HUD rendered every frame like vanilla. 2 = half the rate (30 when that is 60).",
            "Only used while skipUnseenFrames is on; needs FBOs (ignored when disableFbo is on)."
    })
    @Config.RangeInt(min = 1, max = 20)
    public static int hudRateDivisor = 2;

    @Config.Name("disableFbo")
    @Config.Comment({
            "Render the world directly into the window's back buffer instead of an intermediate",
            "framebuffer. This removes one full-screen copy of every frame that is shown, which",
            "helps on weak iGPUs. Vanilla's own framebuffer-less path is used, so the game remains",
            "fully supported; but a few mods assume the framebuffer exists (entity outline glow",
            "effects will not render, screenshots may come out wrong), so this is off by default.",
            "If the world looks black or garbled with your mod pack, turn this back off."
    })
    public static boolean disableFbo = false;
}
