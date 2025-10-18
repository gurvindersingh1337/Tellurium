package vestige.module.impl.visual;

import net.minecraft.client.gui.ScaledResolution;
import vestige.Vestige;
import vestige.event.Listener;
import vestige.event.impl.RenderEvent;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.IntegerSetting;
import vestige.setting.impl.ModeSetting;
import vestige.util.render.FontUtil;
import vestige.util.render.RenderUtil;
import vestige.font.VestigeFontRenderer;
import java.awt.Color;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import java.io.File;

public class IngameInfo extends Module {

    private final ModeSetting font = new ModeSetting("Font", "Comfortaa", FontUtil.getFontSetting().getMode());
    private final BooleanSetting showBPS = new BooleanSetting("BPS", true);
    private final BooleanSetting showBalance = new BooleanSetting("Balance", true);
    private final BooleanSetting showUser = new BooleanSetting("Display Name", true);
    private final BooleanSetting gradientText = new BooleanSetting("Gradient Text", true);
    private final BooleanSetting animatedGradient = new BooleanSetting("Animated Gradient", () -> gradientText.isEnabled(), true);
    private final IntegerSetting gradientSpeed = new IntegerSetting("Gradient Speed", () -> gradientText.isEnabled() && animatedGradient.isEnabled(), 3, 1, 10, 1);
    private final IntegerSetting textColor1R = new IntegerSetting("Text Color 1 Red", () -> gradientText.isEnabled(), 100, 0, 255, 1);
    private final IntegerSetting textColor1G = new IntegerSetting("Text Color 1 Green", () -> gradientText.isEnabled(), 200, 0, 255, 1);
    private final IntegerSetting textColor1B = new IntegerSetting("Text Color 1 Blue", () -> gradientText.isEnabled(), 255, 0, 255, 1);
    private final IntegerSetting textColor2R = new IntegerSetting("Text Color 2 Red", () -> gradientText.isEnabled(), 255, 0, 255, 1);
    private final IntegerSetting textColor2G = new IntegerSetting("Text Color 2 Green", () -> gradientText.isEnabled(), 100, 0, 255, 1);
    private final IntegerSetting textColor2B = new IntegerSetting("Text Color 2 Blue", () -> gradientText.isEnabled(), 200, 0, 255, 1);
    private final BooleanSetting fillGraph = new BooleanSetting("Fill Graph", true);
    private final BooleanSetting showGrid = new BooleanSetting("Show Grid", true);
    private final BooleanSetting glowEffect = new BooleanSetting("Glow Effect", true);
    private final BooleanSetting particles = new BooleanSetting("Speed Particles", true);
    private final BooleanSetting wave = new BooleanSetting("Wave Effect", true);
    private final BooleanSetting peakMarker = new BooleanSetting("Peak Marker", true);
    private final BooleanSetting blur = new BooleanSetting("Background Blur", true);

    private final IntegerSetting graphX = new IntegerSetting("Graph X", 4, 0, 500, 1);
    private final IntegerSetting graphY = new IntegerSetting("Graph Y", 115, 0, 500, 1);
    private final IntegerSetting graphWidth = new IntegerSetting("Graph Width", 60, 40, 200, 5);
    private final IntegerSetting graphHeight = new IntegerSetting("Graph Height", 50, 20, 100, 5);
    private final IntegerSetting graphLineWidth = new IntegerSetting("Line Width", 2, 1, 5, 1);
    private final IntegerSetting glowIntensity = new IntegerSetting("Glow Intensity", 3, 1, 6, 1);

    private final IntegerSetting color1R = new IntegerSetting("Color 1 Red", 0, 0, 255, 1);
    private final IntegerSetting color1G = new IntegerSetting("Color 1 Green", 150, 0, 255, 1);
    private final IntegerSetting color1B = new IntegerSetting("Color 1 Blue", 255, 0, 255, 1);
    private final IntegerSetting color2R = new IntegerSetting("Color 2 Red", 150, 0, 255, 1);
    private final IntegerSetting color2G = new IntegerSetting("Color 2 Green", 150, 0, 255, 1);
    private final IntegerSetting color2B = new IntegerSetting("Color 2 Blue", 150, 0, 255, 1);

    private final IntegerSetting fpsX = new IntegerSetting("FPS X", 4, 0, 500, 1);
    private final IntegerSetting fpsY = new IntegerSetting("FPS Y", 4, 0, 500, 1);
    private final IntegerSetting fpsColorR = new IntegerSetting("FPS Color Red", 255, 0, 255, 1);
    private final IntegerSetting fpsColorG = new IntegerSetting("FPS Color Green", 255, 0, 255, 1);
    private final IntegerSetting fpsColorB = new IntegerSetting("FPS Color Blue", 255, 0, 255, 1);
    private final IntegerSetting pingX = new IntegerSetting("Ping X", 50, 0, 500, 1);
    private final IntegerSetting pingY = new IntegerSetting("Ping Y", 4, 0, 500, 1);
    private final IntegerSetting pingColorR = new IntegerSetting("Ping Color Red", 255, 0, 255, 1);
    private final IntegerSetting pingColorG = new IntegerSetting("Ping Color Green", 255, 0, 255, 1);
    private final IntegerSetting pingColorB = new IntegerSetting("Ping Color Blue", 255, 0, 255, 1);

    private final String username;
    private final boolean isDev;
    private final double[] speedHistory = new double[100];
    private int speedIndex = 0;
    private VestigeFontRenderer comfortaa;
    private java.util.ArrayList<SpeedParticle> speedParticles = new java.util.ArrayList<>();
    private double peakSpeed = 0;
    private int peakIndex = 0;
    private long lastPeakTime = 0;

    public IngameInfo() {
        super("Ingame Info", Category.VISUAL);
        this.addSettings(font, showBPS, showBalance, showUser, gradientText, animatedGradient, gradientSpeed,
                textColor1R, textColor1G, textColor1B, textColor2R, textColor2G, textColor2B,
                fillGraph, showGrid, glowEffect, particles, wave, peakMarker, blur,
                graphX, graphY, graphWidth, graphHeight, graphLineWidth, glowIntensity,
                color1R, color1G, color1B, color2R, color2G, color2B,
                fpsX, fpsY, fpsColorR, fpsColorG, fpsColorB,
                pingX, pingY, pingColorR, pingColorG, pingColorB);

        String userHome = System.getProperty("user.home", "");
        String extractedName = "Unknown";

        if (!userHome.isEmpty()) {
            File homeDir = new File(userHome);
            extractedName = homeDir.getName();
            if (extractedName.isEmpty() || extractedName.equals("")) {
                String[] parts = userHome.split(File.separator.equals("\\") ? "\\\\" : File.separator);
                if (parts.length > 0) {
                    extractedName = parts[parts.length - 1];
                }
            }
        }

        this.username = extractedName;
        this.isDev = extractedName.equalsIgnoreCase("eddy") || extractedName.equalsIgnoreCase("sunset");
    }

    @Override
    public void onClientStarted() {
        comfortaa = Vestige.instance.getFontManager().getComfortaa();
    }

    @Listener
    public void onRender(RenderEvent event) {
        if (mc == null || mc.thePlayer == null || mc.gameSettings.showDebugInfo || comfortaa == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        float x = 4f;
        float y = sr.getScaledHeight() - 14f;

        StringBuilder infoBuilder = new StringBuilder();
        if (showUser.isEnabled()) {
            infoBuilder.append("User ID: ").append(username);
            if (isDev) infoBuilder.append(" (Dev)");
        }
        if (showBPS.isEnabled()) {
            double dx = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
            double dz = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
            double dist = Math.hypot(dx, dz);
            double currentBPS = dist * 20.0 * mc.timer.timerSpeed;
            speedHistory[speedIndex] = currentBPS;
            speedIndex = (speedIndex + 1) % speedHistory.length;
            if (currentBPS > peakSpeed) {
                peakSpeed = currentBPS;
                peakIndex = speedIndex;
                lastPeakTime = System.currentTimeMillis();
            }
            if (particles.isEnabled() && currentBPS > 8.0) {
                if (Math.random() < 0.3) {
                    speedParticles.add(new SpeedParticle(graphX.getValue(), graphY.getValue(), graphWidth.getValue(), graphHeight.getValue()));
                }
            }
            if (infoBuilder.length() > 0) infoBuilder.append(" | ");
            infoBuilder.append(String.format("BPS: %.2f", currentBPS));
        } else {
            double dx = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
            double dz = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
            double dist = Math.hypot(dx, dz);
            double currentBPS = dist * 20.0 * mc.timer.timerSpeed;
            speedHistory[speedIndex] = currentBPS;
            speedIndex = (speedIndex + 1) % speedHistory.length;
        }

        if (showBalance.isEnabled()) {
            if (infoBuilder.length() > 0) infoBuilder.append(" | ");
            infoBuilder.append("Balance: ").append(vestige.Vestige.instance.getBalanceHandler().getBalanceInMS());
        }

        if (infoBuilder.length() > 0) {
            if (gradientText.isEnabled()) {
                drawGradientText(infoBuilder.toString(), x, y);
            } else {
                comfortaa.drawStringWithShadow(infoBuilder.toString(), x, y, -1);
            }
        }

        if (showBPS.isEnabled()) drawSpeedGraph();
        drawFPSAndPing();
    }

    private void drawGradientText(String text, float x, float y) {
        float currentX = x;
        Color color1 = new Color(textColor1R.getValue(), textColor1G.getValue(), textColor1B.getValue());
        Color color2 = new Color(textColor2R.getValue(), textColor2G.getValue(), textColor2B.getValue());
        float time = animatedGradient.isEnabled() ? (System.currentTimeMillis() % 5000) / 5000.0f * gradientSpeed.getValue() : 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            float progress = (float) i / text.length();
            if (animatedGradient.isEnabled()) progress = (progress + time) % 1.0f;
            float smoothProgress = (float) (Math.sin((progress - 0.5) * Math.PI) * 0.5 + 0.5);
            int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * smoothProgress);
            int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * smoothProgress);
            int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * smoothProgress);
            int color = (255 << 24) | (r << 16) | (g << 8) | b;
            comfortaa.drawStringWithShadow(String.valueOf(c), currentX, y, color);
            currentX += comfortaa.getStringWidth(String.valueOf(c));
        }
    }

    private void drawFPSAndPing() {
        int fps = mc.getDebugFPS() * 2;
        int ping = mc.getNetHandler() != null && mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()) != null
                ? mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime()
                : 0;
        Color bracketColor = new Color(150, 150, 150);
        Color fpsColor = new Color(fpsColorR.getValue(), fpsColorG.getValue(), fpsColorB.getValue());
        Color pingColor = new Color(pingColorR.getValue(), pingColorG.getValue(), pingColorB.getValue());
        Color numberColor = Color.WHITE;

        float currentX = fpsX.getValue();
        float currentY = fpsY.getValue();
        comfortaa.drawStringWithShadow("[", currentX, currentY, bracketColor.getRGB());
        currentX += comfortaa.getStringWidth("[");
        comfortaa.drawStringWithShadow("FPS", currentX, currentY, fpsColor.getRGB());
        currentX += comfortaa.getStringWidth("FPS");
        comfortaa.drawStringWithShadow("]", currentX, currentY, bracketColor.getRGB());
        currentX += comfortaa.getStringWidth("]");
        comfortaa.drawStringWithShadow(" " + fps, currentX, currentY, numberColor.getRGB());

        currentX = pingX.getValue();
        currentY = pingY.getValue();
        comfortaa.drawStringWithShadow("[", currentX, currentY, bracketColor.getRGB());
        currentX += comfortaa.getStringWidth("[");
        comfortaa.drawStringWithShadow("Ping", currentX, currentY, pingColor.getRGB());
        currentX += comfortaa.getStringWidth("Ping");
        comfortaa.drawStringWithShadow("]", currentX, currentY, bracketColor.getRGB());
        currentX += comfortaa.getStringWidth("]");
        comfortaa.drawStringWithShadow(" " + ping + "ms", currentX, currentY, numberColor.getRGB());
    }

    private void drawSpeedGraph() {
        float gx = graphX.getValue();
        float gy = graphY.getValue();
        float gw = graphWidth.getValue();
        float gh = graphHeight.getValue();

        if (blur.isEnabled()) RenderUtil.drawRoundedRect(gx - 2, gy - 2, gw + 4, gh + 4, 10, new Color(0, 0, 0, 100).getRGB());
        RenderUtil.drawRoundedRect(gx, gy, gw, gh, 8, new Color(40, 40, 45, 220).getRGB());

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        float time = System.currentTimeMillis() / 1000.0f;

        if (showGrid.isEnabled()) {
            GL11.glLineWidth(0.5f);
            GL11.glBegin(GL11.GL_LINES);
            int gridSize = 10;
            for (int i = 0; i < gw; i += gridSize) {
                float offset = wave.isEnabled() ? (float)Math.sin(time * 2 + i * 0.1) * 0.5f : 0;
                GL11.glColor4f(1f, 1f, 1f, 0.08f + offset * 0.02f);
                GL11.glVertex2f(gx + i, gy);
                GL11.glVertex2f(gx + i, gy + gh);
            }
            for (int i = 0; i < gh; i += gridSize) {
                float offset = wave.isEnabled() ? (float)Math.sin(time * 2 + i * 0.1) * 0.5f : 0;
                GL11.glColor4f(1f, 1f, 1f, 0.08f + offset * 0.02f);
                GL11.glVertex2f(gx, gy + i);
                GL11.glVertex2f(gx + gw, gy + i);
            }
            GL11.glEnd();
        }

        double maxSpeed = 0;
        for (double speed : speedHistory) if (speed > maxSpeed) maxSpeed = speed;
        if (maxSpeed == 0) maxSpeed = 1;

        Color blue = new Color(color1R.getValue(), color1G.getValue(), color1B.getValue());
        Color grey = new Color(color2R.getValue(), color2G.getValue(), color2B.getValue());

        if (glowEffect.isEnabled()) {
            int intensity = glowIntensity.getValue();
            for (int layer = intensity; layer > 0; layer--) {
                float glowWidth = graphLineWidth.getValue() + (layer * 2);
                GL11.glLineWidth(glowWidth);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                for (int i = 0; i < speedHistory.length; i++) {
                    int index = (speedIndex + i) % speedHistory.length;
                    double speed = speedHistory[index];
                    float px = gx + 4 + (i / (float) speedHistory.length) * (gw - 8);
                    float py = gy + gh - (float) ((speed / maxSpeed) * (gh - 8)) - 4;
                    if (wave.isEnabled()) py += (float)Math.sin(time * 3 + i * 0.2) * 1.5f;
                    float t = i / (float) speedHistory.length;
                    int r = (int) (blue.getRed() * (1 - t) + grey.getRed() * t);
                    int g = (int) (blue.getGreen() * (1 - t) + grey.getGreen() * t);
                    int b = (int) (blue.getBlue() * (1 - t) + grey.getBlue() * t);
                    float alpha = 0.2f / layer;
                    GL11.glColor4f(r / 255f, g / 255f, b / 255f, alpha);
                    GL11.glVertex2f(px, py);
                }
                GL11.glEnd();
            }
        }

        if (fillGraph.isEnabled()) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int i = 0; i < speedHistory.length; i++) {
                int index = (speedIndex + i) % speedHistory.length;
                double speed = speedHistory[index];
                float px = gx + 4 + (i / (float) speedHistory.length) * (gw - 8);
                float py = gy + gh - (float) ((speed / maxSpeed) * (gh - 8)) - 4;
                if (wave.isEnabled()) py += (float)Math.sin(time * 3 + i * 0.2) * 1.5f;
                float t = i / (float) speedHistory.length;
                int r = (int) (blue.getRed() * (1 - t) + grey.getRed() * t);
                int g = (int) (blue.getGreen() * (1 - t) + grey.getGreen() * t);
                int b = (int) (blue.getBlue() * (1 - t) + grey.getBlue() * t);
                float pulse = wave.isEnabled() ? (float)Math.sin(time * 4 + i * 0.15) * 0.1f + 0.9f : 1.0f;
                GL11.glColor4f(r / 255f, g / 255f, b / 255f, 0.15f * pulse);
                GL11.glVertex2f(px, gy + gh - 4);
                GL11.glColor4f(r / 255f, g / 255f, b / 255f, 0.7f * pulse);
                GL11.glVertex2f(px, py);
            }
            GL11.glEnd();
        }

        GL11.glLineWidth(graphLineWidth.getValue());
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < speedHistory.length; i++) {
            int index = (speedIndex + i) % speedHistory.length;
            double speed = speedHistory[index];
            float px = gx + 4 + (i / (float) speedHistory.length) * (gw - 8);
            float py = gy + gh - (float) ((speed / maxSpeed) * (gh - 8)) - 4;
            if (wave.isEnabled()) py += (float)Math.sin(time * 3 + i * 0.2) * 1.5f;
            float t = i / (float) speedHistory.length;
            int r = (int) (blue.getRed() * (1 - t) + grey.getRed() * t);
            int g = (int) (blue.getGreen() * (1 - t) + grey.getGreen() * t);
            int b = (int) (blue.getBlue() * (1 - t) + grey.getBlue() * t);
            GL11.glColor4f(r / 255f, g / 255f, b / 255f, 1f);
            GL11.glVertex2f(px, py);
        }
        GL11.glEnd();

        if (peakMarker.isEnabled() && System.currentTimeMillis() - lastPeakTime < 3000) {
            int peakPos = (peakIndex - speedIndex + speedHistory.length) % speedHistory.length;
            float px = gx + 4 + (peakPos / (float) speedHistory.length) * (gw - 8);
            float py = gy + gh - (float) ((peakSpeed / maxSpeed) * (gh - 8)) - 4;
            if (wave.isEnabled()) py += (float)Math.sin(time * 3 + peakPos * 0.2) * 1.5f;
            float fade = 1.0f - ((System.currentTimeMillis() - lastPeakTime) / 3000.0f);
            float markerPulse = (float)Math.sin(time * 8) * 0.3f + 0.7f;
            GL11.glPointSize(10.0f * markerPulse);
            GL11.glBegin(GL11.GL_POINTS);
            GL11.glColor4f(1f, 1f, 1f, fade * 0.9f);
            GL11.glVertex2f(px, py);
            GL11.glEnd();
            GL11.glPointSize(6.0f);
            GL11.glBegin(GL11.GL_POINTS);
            GL11.glColor4f(1f, 0.3f, 0.3f, fade);
            GL11.glVertex2f(px, py);
            GL11.glEnd();
        }

        if (particles.isEnabled()) {
            java.util.Iterator<SpeedParticle> iterator = speedParticles.iterator();
            while (iterator.hasNext()) {
                SpeedParticle particle = iterator.next();
                particle.update();
                if (particle.alpha <= 0) {
                    iterator.remove();
                    continue;
                }
                GL11.glPointSize(particle.size);
                GL11.glBegin(GL11.GL_POINTS);
                float t = particle.lifetime / 60.0f;
                int r = (int) (blue.getRed() * (1 - t) + grey.getRed() * t);
                int g = (int) (blue.getGreen() * (1 - t) + grey.getGreen() * t);
                int b = (int) (blue.getBlue() * (1 - t) + grey.getBlue() * t);
                GL11.glColor4f(r / 255f, g / 255f, b / 255f, particle.alpha);
                GL11.glVertex2f(particle.x, particle.y);
                GL11.glEnd();
            }
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static class SpeedParticle {
        float x, y;
        float vx, vy;
        float alpha;
        float size;
        int lifetime;
        public SpeedParticle(float gx, float gy, float gw, float gh) {
            this.x = gx + (float)(Math.random() * gw);
            this.y = gy + gh - (float)(Math.random() * gh * 0.5);
            this.vx = (float)(Math.random() * 2 - 1);
            this.vy = (float)(Math.random() * -2 - 1);
            this.alpha = 1.0f;
            this.size = (float)(Math.random() * 3 + 2);
            this.lifetime = 0;
        }
        public void update() {
            x += vx;
            y += vy;
            vy += 0.1f;
            alpha -= 0.02f;
            lifetime++;
        }
    }
}
