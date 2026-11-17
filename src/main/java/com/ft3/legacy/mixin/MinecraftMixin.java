package com.ft3.legacy.mixin;

import com.ft3.legacy.FramePacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * 1.12.2 equivalent of the original mod's {@code RenderSystem#flipFrame} redirect.
 * <p>
 * Vanilla ends every frame with {@code Display.update()}, which swaps the buffers (puts the frame
 * on screen) and then polls input/window events. We keep the event polling on every frame so the
 * game stays responsive, but only swap when {@link FramePacer} says this frame is the one to show.
 * Skipped frames are discarded; the next frame draws over the same back buffer.
 * <p>
 * With {@code skipUnseenFrames} on, the two game-loop gates go further: iterations whose frame
 * would never be shown do not render the world at all and do not blit it to the window, so the
 * loop runs at tick speed, and the one frame per presentation slot is drawn fresh from the newest
 * input and tick state, then shown immediately.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public boolean skipRenderWorld;

    @Redirect(method = "updateDisplay",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;update()V", remap = false))
    private void ft3$presentOnlyLatestFrame() {
        if (FramePacer.shouldPresent(skipRenderWorld)) {
            Display.update();
        } else {
            Display.processMessages();
        }
    }

    @WrapOperation(method = "runGameLoop",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V"))
    private static void ft3$renderOnlyVisibleFrame(EntityRenderer renderer, float partialTicks,
                                                   long nanoTime, Operation<Void> original) {
        // Any open GUI screen always renders: skipping would freeze its visuals while clicks
        // still register, which is never what the user wants.
        if (FramePacer.shouldRenderFrame(Minecraft.getMinecraft().currentScreen != null)) {
            original.call(renderer, partialTicks, nanoTime);
        }
    }

    @WrapOperation(method = "runGameLoop",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V"))
    private static void ft3$blitOnlyVisibleFrame(Framebuffer framebuffer, int width, int height,
                                                 Operation<Void> original) {
        // Same decision as the render gate, made once per iteration: skipped iterations keep the
        // window's back buffer as-is, since it will not be swapped anyway. When vanilla skips the
        // world render (loading screens), the render gate never ran this iteration, so always blit
        // like vanilla does.
        if (FramePacer.isRenderIteration() || Minecraft.getMinecraft().skipRenderWorld) {
            original.call(framebuffer, width, height);
        }
    }
}
