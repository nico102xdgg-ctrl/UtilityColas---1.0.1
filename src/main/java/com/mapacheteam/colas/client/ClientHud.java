package com.mapacheteam.colas.client;

import com.mapacheteam.colas.ColasMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class ClientHud {
    private static final ResourceLocation TRUE = tex("colatrue");
    private static final ResourceLocation FALSE = tex("colafalse");
    private static final ResourceLocation ROJO = tex("rojo");
    private static final ResourceLocation AMARILLO = tex("amarillo");

    private static int hud = 0;       // 0 ninguna, 1 colatrue, 2 colafalse
    private static int tint = 0;      // 0 ninguna, 1 amarillo, 2 rojo
    private static int tintLeft = 0;

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(ColasMod.MODID, "textures/gui/" + n + ".png");
    }

    public static void apply(int h, int t, int ticks) {
        if (h >= 0) hud = h;
        if (t >= 0) {
            tint = t;
            tintLeft = t == 0 ? 0 : ticks;
        }
    }

    public static void tick() {
        if (tintLeft > 0 && --tintLeft == 0) tint = 0;
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1) imagen de color (amarillo / rojo) a pantalla completa, con fade en el ultimo segundo
        if (tint != 0 && tintLeft > 0) {
            float a = Math.min(1f, tintLeft / 20f);
            RenderSystem.setShaderColor(1f, 1f, 1f, a);
            g.blit(tint == 1 ? AMARILLO : ROJO, 0, 0, w, h, 0f, 0f, 16, 16, 16, 16);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        // 2) interfaz (encima de la imagen, se ven las dos)
        if (hud != 0) {
            int iw = 16 * 4, ih = 14 * 4;
            g.blit(hud == 1 ? TRUE : FALSE, (w - iw) / 2, 10, iw, ih, 0f, 0f, 16, 14, 16, 14);
        }
        RenderSystem.disableBlend();
    }
}
