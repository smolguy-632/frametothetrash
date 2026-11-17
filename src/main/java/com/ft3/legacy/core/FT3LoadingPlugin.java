package com.ft3.legacy.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

/**
 * Early (coremod) entry point. Needed because the mixin targets {@code net.minecraft.client.Minecraft},
 * a vanilla class, which has to be patched before it is loaded. MixinBooter reads
 * {@link #getMixinConfigs()} and hands the config to the Mixin library. Celeritas loads the same way.
 */
@IFMLLoadingPlugin.Name("FrameToTheTrash")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class FT3LoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.frametothetrash.json");
    }
}
