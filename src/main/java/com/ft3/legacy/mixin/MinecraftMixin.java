package com.ft3.legacy.mixin;

import com.ft3.legacy.FramePacer;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.12.2 equivalent of the original mod's {@code RenderSystem#flipFrame} redirect.
 * <p>
 * Vanilla ends every frame with {@code Display.update()}, which swaps the buffers (puts the frame
 * on screen) and then polls input/window events. We keep the event polling on every frame so the
 * game stays responsive, but only swap when {@link FramePacer} says this frame is the one to show.
 * Skipped frames are discarded; the next frame draws over the same back buffer.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Redirect(method = "updateDisplay",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;update()V", remap = false))
    private void ft3$presentOnlyLatestFrame() {
        if (FramePacer.shouldPresent()) {
            Display.update();
        } else {
            Display.processMessages();
        }
    }
}
