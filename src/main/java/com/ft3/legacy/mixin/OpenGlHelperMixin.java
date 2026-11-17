package com.ft3.legacy.mixin;

import com.ft3.legacy.FT3Config;
import net.minecraft.client.renderer.OpenGlHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The {@code disableFbo} option.
 * <p>
 * {@code OpenGlHelper.isFramebufferEnabled()} is the single switch vanilla itself uses to decide
 * between the framebuffer path (world rendered into an off-screen framebuffer, then copied to the
 * window with a full-screen blit every frame) and the direct path (world rendered straight into
 * the window's back buffer). Every Framebuffer method is internally guarded by this check, so
 * returning {@code false} here cleanly switches the whole game to the direct path: no intermediate
 * framebuffer, no per-frame blit. That is one less full-screen copy of every frame that is shown,
 * which helps on weak iGPUs.
 * <p>
 * This only forces the answer when the option is on; otherwise vanilla's check decides untouched.
 */
@Mixin(OpenGlHelper.class)
public abstract class OpenGlHelperMixin {

    @Inject(method = "isFramebufferEnabled", at = @At("HEAD"), cancellable = true)
    private static void ft3$disableFbo(CallbackInfoReturnable<Boolean> cir) {
        if (FT3Config.disableFbo) {
            cir.setReturnValue(false);
        }
    }
}
