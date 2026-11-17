package com.ft3.legacy.client;

import com.ft3.legacy.Tags;
import net.minecraftforge.fml.client.DefaultGuiFactory;

/** Gives the mod a "Config" button in the Mods menu, generated from {@link com.ft3.legacy.FT3Config}. */
public class FT3GuiFactory extends DefaultGuiFactory {

    public FT3GuiFactory() {
        super(Tags.MODID, Tags.MODNAME);
    }
}
