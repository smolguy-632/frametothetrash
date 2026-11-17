package com.ft3.legacy.mixin;

import com.ft3.legacy.HudCache;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Exordium-style HUD caching (see {@link HudCache}). {@code GuiIngame.renderGameOverlay} is the
 * single call that draws the whole in-game HUD (hotbar, crosshair, chat, potion icons, boss bars)
 * inside {@code EntityRenderer.updateCameraAndRender}, once per rendered frame. Wrapping it lets
 * {@link HudCache} redirect the draw into its own framebuffer on HUD slots and stamp the cached
 * image over every frame in between, so the HUD does not have to be drawn at the full frame rate.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @WrapOperation(method = "updateCameraAndRender",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V"))
    private void ft3$cachedHud(GuiIngame ingameGui, float partialTicks, Operation<Void> original) {
        HudCache.renderHud(ingameGui, partialTicks, original);
    }
}
