package vestige.module.impl.visual;

import vestige.Vestige;
import vestige.font.VestigeFontRenderer;
import vestige.module.AlignType;
import vestige.module.Category;
import vestige.module.HUDModule;
import vestige.setting.impl.IntegerSetting;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.ModeSetting;
import vestige.util.render.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.lwjgl.opengl.GL11.*;

public class Watermark extends HUDModule {

    private final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Name");
    private final BooleanSetting showTime = new BooleanSetting("Show Time", true);
    private final BooleanSetting showFPS = new BooleanSetting("Show FPS", true);
    private final BooleanSetting showServer = new BooleanSetting("Show Server", true);
    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final BooleanSetting underline = new BooleanSetting("Underline", true);
    private final BooleanSetting roundedCorners = new BooleanSetting("Rounded Corners", true);
    private final BooleanSetting glowEffect = new BooleanSetting("Glow Effect", false);

    private final IntegerSetting color1R = new IntegerSetting("Color 1 Red", 100, 0, 255, 1);
    private final IntegerSetting color1G = new IntegerSetting("Color 1 Green", 150, 0, 255, 1);
    private final IntegerSetting color1B = new IntegerSetting("Color 1 Blue", 255, 0, 255, 1);

    private final IntegerSetting color2R = new IntegerSetting("Color 2 Red", 160, 0, 255, 1);
    private final IntegerSetting color2G = new IntegerSetting("Color 2 Green", 160, 0, 255, 1);
    private final IntegerSetting color2B = new IntegerSetting("Color 2 Blue", 165, 0, 255, 1);

    private final IntegerSetting nameFontSize = new IntegerSetting("Name Font Size", 40, 10, 100, 1);
    private final IntegerSetting opaiScale = new IntegerSetting("Opai Scale", 50, 10, 200, 1);

    private VestigeFontRenderer productSans;
    private VestigeFontRenderer comfortaa;
    private Minecraft mc = Minecraft.getMinecraft();
    private ResourceLocation opaiLogo = new ResourceLocation("minecraft", "lycanis/image/Opai.png");

    public Watermark() {
        super("Watermark", Category.VISUAL, 4, 4, 100, 20, AlignType.LEFT);
        this.addSettings(mode, showTime, showFPS, showServer, background, underline, color1R, color1G, color1B, color2R, color2G, color2B, nameFontSize, opaiScale);
        this.setEnabledSilently(true);
    }

    @Override
    public void onClientStarted() {
        productSans = Vestige.instance.getFontManager().getProductSans();
        comfortaa = Vestige.instance.getFontManager().getComfortaa();
    }

    @Override
    protected void renderModule(boolean inChat) {
        if(mode.getMode().equals("Opai")) {
            renderOpaiMode();
        } else if(mode.getMode().equals("Name")) {
            renderNameMode();
        } else {
            renderNormalMode();
        }
    }

    private void renderOpaiMode() {
        float x = (float) posX.getValue();
        float y = (float) posY.getValue();

        int size = opaiScale.getValue();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        mc.getTextureManager().bindTexture(opaiLogo);

        Gui.drawModalRectWithCustomSizedTexture((int)x, (int)y, 0, 0, size, size, size, size);

        GlStateManager.disableBlend();

        width = size;
        height = size;
    }

    private void drawGradientGlow(float x, float y, float width, float height) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        long time = System.currentTimeMillis();
        float offset = (time % 3000) / 3000.0f;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        Color color1 = getAnimatedColor(offset);
        Color color2 = getAnimatedColor(offset + 0.25f);
        Color color3 = getAnimatedColor(offset + 0.5f);
        Color color4 = getAnimatedColor(offset + 0.75f);

        worldrenderer.pos(x + width, y, 0.0D).color(color2.getRed(), color2.getGreen(), color2.getBlue(), 60).endVertex();
        worldrenderer.pos(x, y, 0.0D).color(color1.getRed(), color1.getGreen(), color1.getBlue(), 60).endVertex();
        worldrenderer.pos(x, y + height, 0.0D).color(color4.getRed(), color4.getGreen(), color4.getBlue(), 60).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).color(color3.getRed(), color3.getGreen(), color3.getBlue(), 60).endVertex();

        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private Color getAnimatedColor(float offset) {
        offset = offset % 1.0f;
        float hue = offset;
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    private void renderNormalMode() {
        if(productSans == null) return;

        float x = (float) posX.getValue();
        float y = (float) posY.getValue();
        float currentX = x;

        String prefix = "Tellurium";
        StringBuilder suffixBuilder = new StringBuilder();

        if(showFPS.isEnabled()) {
            int fps = Minecraft.getDebugFPS() * 2;
            suffixBuilder.append(" | ").append(fps).append(" fps");
        }

        if(showServer.isEnabled()) {
            ServerData serverData = mc.getCurrentServerData();
            String serverIP = serverData != null ? serverData.serverIP : "Singleplayer";
            suffixBuilder.append(" | ").append(serverIP);
        }

        if(showTime.isEnabled()) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
            suffixBuilder.append(" | ").append(timeFormat.format(new Date()));
        }

        String suffix = suffixBuilder.toString();
        float totalWidth = (float) productSans.getStringWidth(prefix + suffix);

        if(background.isEnabled()) {
            Gui.drawRect(x - 3, y - 3, x + totalWidth + 3, y + 10 + 3, new Color(0, 0, 0, 120).getRGB());
        }

        Color color1 = new Color(color1R.getValue(), color1G.getValue(), color1B.getValue());
        Color color2 = new Color(color2R.getValue(), color2G.getValue(), color2B.getValue());

        for(int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            float progress = prefix.length() == 1 ? 0 : (float) i / (prefix.length() - 1);
            Color color = ColorUtil.getGradient(color1, color2, progress);
            productSans.drawStringWithShadow(String.valueOf(c), currentX, y, color.getRGB());
            currentX += (float) productSans.getStringWidth(String.valueOf(c));
        }

        if(!suffix.isEmpty()) {
            productSans.drawStringWithShadow(suffix, currentX, y, 0xFFFFFFFF);
        }

        if(underline.isEnabled()) {
            Color gradientStart = new Color(color1R.getValue(), color1G.getValue(), color1B.getValue());
            Color gradientEnd = new Color(color2R.getValue(), color2G.getValue(), color2B.getValue());

            for(int i = 0; i < totalWidth; i++) {
                float progress = (float) i / totalWidth;
                Color lineColor = ColorUtil.getGradient(gradientStart, gradientEnd, progress);
                Gui.drawRect(x - 3 + i, y + 11, x - 3 + i + 1, y + 13, lineColor.getRGB());
            }
        }

        width = (int) (totalWidth + 6);
        height = 16;
    }

    private void renderNameMode() {
        if(comfortaa == null) {
            comfortaa = Vestige.instance.getFontManager().getComfortaa();
            if(comfortaa == null) return;
        }

        float x = (float) posX.getValue();
        float y = (float) posY.getValue();

        String text = "Tellurium";
        float scale = nameFontSize.getValue() / 20.0f;

        glPushMatrix();
        glTranslatef(x, y, 0);
        glScalef(scale, scale, 1);

        float currentX = 0;

        Color color1 = new Color(color1R.getValue(), color1G.getValue(), color1B.getValue());
        Color color2 = new Color(color2R.getValue(), color2G.getValue(), color2B.getValue());

        for(int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float progress = text.length() == 1 ? 0 : (float) i / (text.length() - 1);
            Color charColor = ColorUtil.getGradient(color1, color2, progress);
            comfortaa.drawStringWithShadow(String.valueOf(c), currentX, 0, charColor.getRGB());
            currentX += comfortaa.getStringWidth(String.valueOf(c));
        }

        glPopMatrix();

        width = (int) (currentX * scale);
        height = (int) (comfortaa.getHeight() * scale);
    }

    private void drawGradientLine(float x, float y, float width, float height, Color startColor, Color endColor) {
        for(int i = 0; i < width; i++) {
            float progress = i / width;
            Color color = ColorUtil.getGradient(startColor, endColor, progress);
            Gui.drawRect(x + i, y, x + i + 1, y + height, color.getRGB());
        }
    }

    private void drawGlowRect(float x, float y, float width, float height, float radius, Color color) {
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float a = color.getAlpha() / 255.0F;
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        glColor4f(r, g, b, a);
        glEnable(GL_LINE_SMOOTH);

        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(x + width / 2, y + height / 2);

        for(int i = 0; i <= 360; i += 5) {
            double angle = Math.toRadians(i);
            if(i <= 90) {
                glVertex2d(x + radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius);
            } else if(i <= 180) {
                glVertex2d(x + radius + Math.cos(angle) * radius, y + height - radius + Math.sin(angle) * radius);
            } else if(i <= 270) {
                glVertex2d(x + width - radius + Math.cos(angle) * radius, y + height - radius + Math.sin(angle) * radius);
            } else {
                glVertex2d(x + width - radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius);
            }
        }
        glEnd();

        glDisable(GL_LINE_SMOOTH);
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.disableBlend();
    }

    private void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

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
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.disableBlend();
    }
}