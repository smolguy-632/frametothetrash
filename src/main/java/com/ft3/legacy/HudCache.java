package com.ft3.legacy;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;

/**
 * Exordium-style HUD caching for the {@code hudRateDivisor} option.
 * <p>
 * The in-game HUD (hotbar, crosshair, chat, potion icons, boss bars...) is drawn into its own
 * framebuffer only once per HUD slot ({@code updatesPerSecond / hudRateDivisor} times per second).
 * Every other rendered frame just stamps that cached image over the freshly rendered world, so the
 * HUD does not have to be drawn at the full frame rate. The HUD does not need to be responsive:
 * its content changes rarely, and the debug F3 overlay is drawn outside this call anyway.
 * <p>
 * All of this only applies while the mod is enabled, display decoupling is on, the divisor is
 * greater than 1, and FBOs are in use; otherwise the vanilla HUD render passes through untouched.
 */
public final class HudCache {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final int GL_COLOR_BUFFER_BIT = 16384;
    private static final int GL_DEPTH_BUFFER_BIT = 256;
    private static final int GL_PROJECTION = 5889;
    private static final int GL_MODELVIEW = 5888;

    /** Own framebuffer holding the last drawn HUD, or null until first used. */
    private static Framebuffer hudFramebuffer;
    /** Deadline (System.nanoTime) of the next HUD redraw, 0 = redraw immediately. */
    private static long nextHudNanos;
    /** True when {@code hudFramebuffer} holds a usable HUD image. */
    private static boolean cached;

    private HudCache() {}

    /**
     * Called instead of the vanilla HUD render inside {@code EntityRenderer.updateCameraAndRender},
     * which runs once per rendered frame.
     */
    public static void renderHud(GuiIngame ingameGui, float partialTicks, Operation<Void> original) {
        if (!FT3Config.enabled || !FT3Config.skipUnseenFrames
                || FT3Config.hudRateDivisor <= 1
                || !OpenGlHelper.isFramebufferEnabled()) {
            original.call(ingameGui, partialTicks);
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (ensureFramebuffer(mc)) {
            // (re)created for a new window size: redraw immediately
            nextHudNanos = 0L;
        }

        final long now = System.nanoTime();
        final long interval = NANOS_PER_SECOND / Math.max(1, FT3Config.updatesPerSecond)
                * Math.max(1, FT3Config.hudRateDivisor);

        if (nextHudNanos == 0L || now >= nextHudNanos) {
            nextHudNanos = now + interval;

            // draw the HUD into our own framebuffer instead of the game framebuffer
            hudFramebuffer.bindFramebuffer(true);
            GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GlStateManager.clearDepth(1.0D);
            GlStateManager.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            original.call(ingameGui, partialTicks);
            hudFramebuffer.unbindFramebuffer();
            mc.getFramebuffer().bindFramebuffer(true);
            blit(mc);
            cached = true;
        } else if (cached) {
            blit(mc);
        } else {
            // nothing usable cached yet (should not happen, but never lose the HUD)
            original.call(ingameGui, partialTicks);
        }
    }

    /** @return true when the framebuffer was created or recreated (size changed). */
    private static boolean ensureFramebuffer(Minecraft mc) {
        int width = mc.displayWidth;
        int height = mc.displayHeight;
        if (hudFramebuffer == null) {
            hudFramebuffer = new Framebuffer(width, height, true);
            return true;
        }
        if (hudFramebuffer.framebufferWidth != width
                || hudFramebuffer.framebufferHeight != height) {
            hudFramebuffer.deleteFramebuffer();
            hudFramebuffer = new Framebuffer(width, height, true);
            return true;
        }
        return false;
    }

    /** Draws the cached HUD image over the currently bound (game) framebuffer. */
    private static void blit(Minecraft mc) {
        int width = mc.displayWidth;
        int height = mc.displayHeight;
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);
        GlStateManager.viewport(0, 0, width, height);
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        hudFramebuffer.bindFramebufferTexture();
        float texW = (float) width / hudFramebuffer.framebufferTextureWidth;
        float texH = (float) height / hudFramebuffer.framebufferTextureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0.0D, height, 0.0D).tex(0.0D, 0.0D).endVertex();
        buffer.pos(width, height, 0.0D).tex(texW, 0.0D).endVertex();
        buffer.pos(width, 0.0D, 0.0D).tex(texW, texH).endVertex();
        buffer.pos(0.0D, 0.0D, 0.0D).tex(0.0D, texH).endVertex();
        tessellator.draw();
        hudFramebuffer.unbindFramebufferTexture();
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
    }
}
