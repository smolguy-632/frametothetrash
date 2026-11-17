package com.ft3.legacy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Frame to the Trash, ported to Minecraft 1.12.2 (Forge).
 * Original mod by NegativeOneHero (CC0-1.0).
 * <p>
 * The actual work happens in {@link com.ft3.legacy.mixin.MinecraftMixin} and {@link FramePacer};
 * this class only owns the config and the log line.
 */
@Mod(modid = Tags.MODID,
        name = Tags.MODNAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        clientSideOnly = true,
        guiFactory = "com.ft3.legacy.client.FT3GuiFactory",
        dependencies = "required-after:mixinbooter@[8.0,);")
public class FrameToTheTrash {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Frame to the Trash! Presenting {} frame(s) per second (enabled: {}).",
                FT3Config.updatesPerSecond, FT3Config.enabled);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Tags.MODID.equals(event.getModID())) {
            ConfigManager.sync(Tags.MODID, Config.Type.INSTANCE);
        }
    }
}
