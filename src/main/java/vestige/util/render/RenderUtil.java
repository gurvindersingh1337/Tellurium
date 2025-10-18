package vestige.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

import static org.lwjgl.opengl.GL11.*;

public class RenderUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void prepareBoxRender(float lineWidth, double red, double green, double blue, double alpha) {
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(lineWidth);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDepthMask(false);

        GL11.glColor4d(red, green, blue, alpha);
    }

    public static void renderEntityBox(RenderManager rm, float partialTicks, Entity entity) {
        AxisAlignedBB bb = entity.getEntityBoundingBox();

        double posX = interpolate(entity.posX, entity.lastTickPosX, partialTicks);
        double posY = interpolate(entity.posY, entity.lastTickPosY, partialTicks);
        double posZ = interpolate(entity.posZ, entity.lastTickPosZ, partialTicks);

        RenderGlobal.drawSelectionBoundingBox(
                new AxisAlignedBB(
                        bb.minX - 0.05 - entity.posX + (posX - rm.renderPosX),
                        bb.minY - 0.05 - entity.posY + (posY - rm.renderPosY),
                        bb.minZ - 0.05 - entity.posZ + (posZ - rm.renderPosZ),
                        bb.maxX + 0.05 - entity.posX + (posX - rm.renderPosX),
                        bb.maxY + 0.1 - entity.posY + (posY - rm.renderPosY),
                        bb.maxZ + 0.05 - entity.posZ + (posZ - rm.renderPosZ)
                )
        );
    }

    public static void renderCustomPlayerBox(RenderManager rm, float partialTicks, double x, double y, double z) {
        renderCustomPlayerBox(rm, partialTicks, x, y, z, x, y, z);
    }

    public static void renderCustomPlayerBox(RenderManager rm, float partialTicks, double x, double y, double z, double lastX, double lastY, double lastZ) {
        AxisAlignedBB bb = new AxisAlignedBB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);

        double posX = interpolate(x, lastX, partialTicks);
        double posY = interpolate(y, lastY, partialTicks);
        double posZ = interpolate(z, lastZ, partialTicks);

        RenderGlobal.drawSelectionBoundingBox(
                new AxisAlignedBB(
                        bb.minX - 0.05 - x + (posX - rm.renderPosX),
                        bb.minY - 0.05 - y + (posY - rm.renderPosY),
                        bb.minZ - 0.05 - z + (posZ - rm.renderPosZ),
                        bb.maxX + 0.05 - x + (posX - rm.renderPosX),
                        bb.maxY + 0.1 - y + (posY - rm.renderPosY),
                        bb.maxZ + 0.05 - z + (posZ - rm.renderPosZ)
                )
        );
    }

    public static void stopBoxRender() {
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4d(1, 1, 1, 1);
    }

    public static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        glColor4f(r, g, b, a);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(x + width / 2, y + height / 2);

        for(int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i + 180);
            glVertex2d(x + radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        for(int i = 90; i <= 180; i += 3) {
            double angle = Math.toRadians(i + 180);
            glVertex2d(x + radius + Math.sin(angle) * radius, y + height - radius + Math.cos(angle) * radius);
        }

        for(int i = 180; i <= 270; i += 3) {
            double angle = Math.toRadians(i + 180);
            glVertex2d(x + width - radius + Math.sin(angle) * radius, y + height - radius + Math.cos(angle) * radius);
        }

        for(int i = 270; i <= 360; i += 3) {
            double angle = Math.toRadians(i + 180);
            glVertex2d(x + width - radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        glVertex2d(x + radius, y);
        glEnd();

        glDisable(GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius, float lineWidth, Color color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        glLineWidth(lineWidth);

        glBegin(GL_LINE_LOOP);

        for(int i = 180; i <= 270; i += 3) {
            double angle = Math.toRadians(i);
            glVertex2d(x + radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        for(int i = 270; i <= 360; i += 3) {
            double angle = Math.toRadians(i);
            glVertex2d(x + width - radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        for(int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i);
            glVertex2d(x + width - radius + Math.sin(angle) * radius, y + height - radius + Math.cos(angle) * radius);
        }

        for(int i = 90; i <= 180; i += 3) {
            double angle = Math.toRadians(i);
            glVertex2d(x + radius + Math.sin(angle) * radius, y + height - radius + Math.cos(angle) * radius);
        }

        glEnd();

        glDisable(GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedGradient(float x, float y, float width, float height, float radius, Color c1, Color c2) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL_SMOOTH);

        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        glBegin(GL_TRIANGLE_FAN);

        glColor4f(c1.getRed() / 255f, c1.getGreen() / 255f, c1.getBlue() / 255f, c1.getAlpha() / 255f);
        glVertex2f(x + width / 2, y + height / 2);

        for(int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i + 180);
            float ratio = i / 360f;
            Color color = interpolateColor(c1, c2, ratio);
            glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            glVertex2d(x + radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        for(int i = 90; i <= 180; i += 3) {
            double angle = Math.toRadians(i + 180);
            float ratio = i / 360f;
            Color color = interpolateColor(c1, c2, ratio);
            glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            glVertex2d(x + radius + Math.sin(angle) * radius, y + height - radius + Math.cos(angle) * radius);
        }

        for(int i = 180; i <= 270; i += 3) {
            double angle = Math.toRadians(i + 180);
            float ratio = i / 360f;
            Color color = interpolateColor(c1, c2, ratio);
            glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            glVertex2d(x + width - radius + Math.sin(angle) * radius, y + height - radius + Math.cos(angle) * radius);
        }

        for(int i = 270; i <= 360; i += 3) {
            double angle = Math.toRadians(i + 180);
            float ratio = i / 360f;
            Color color = interpolateColor(c1, c2, ratio);
            glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            glVertex2d(x + width - radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        glColor4f(c1.getRed() / 255f, c1.getGreen() / 255f, c1.getBlue() / 255f, c1.getAlpha() / 255f);
        glVertex2d(x + radius, y);

        glEnd();

        glDisable(GL_LINE_SMOOTH);
        GlStateManager.shadeModel(GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static Color interpolateColor(Color c1, Color c2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
        int a = (int)(c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * ratio);
        return new Color(r, g, b, a);
    }

    // ===== Added drawNametag =====
    public static void drawNametag(String text, double x, double y, double z, float scale, Color color) {
        RenderManager renderManager = mc.getRenderManager();
        FontRenderer fontRenderer = mc.fontRendererObj;
        double posX = x - renderManager.viewerPosX;
        double posY = y - renderManager.viewerPosY;
        double posZ = z - renderManager.viewerPosZ;

        GL11.glPushMatrix();
        GL11.glTranslatef((float) posX, (float) posY + 0.7f, (float) posZ);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();

        int width = fontRenderer.getStringWidth(text) / 2;
        glBegin(GL_QUADS);
        glColor4f(0, 0, 0, 0.5f);
        glVertex2f(-width - 2, -2);
        glVertex2f(-width - 2, 10);
        glVertex2f(width + 2, 10);
        glVertex2f(width + 2, -2);
        glEnd();

        fontRenderer.drawString(text, -width, 0, color.getRGB(), true);

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
    }

}
