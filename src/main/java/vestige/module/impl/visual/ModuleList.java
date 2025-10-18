package vestige.module.impl.visual;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.StringUtils;
import vestige.Vestige;
import vestige.event.Listener;
import vestige.event.impl.PostMotionEvent;
import vestige.font.VestigeFontRenderer;
import vestige.module.*;
import vestige.setting.impl.*;
import vestige.util.animation.AnimationHolder;
import vestige.util.animation.AnimationType;

import java.awt.*;
import java.util.ArrayList;

public class ModuleList extends HUDModule {

    private boolean initialised;
    private final ArrayList<AnimationHolder<Module>> modules = new ArrayList<>();

    // Core settings
    private final BooleanSetting adjustMode = new BooleanSetting("Adjust Mode", false);
    private final EnumModeSetting<AlignType> alignMode = new EnumModeSetting<>("Align", AlignType.RIGHT, AlignType.values());
    private final IntegerSetting animSpeed = new IntegerSetting("Animation Speed", 350, 50, 1000, 50);
    private final EnumModeSetting<AnimationMode> animationMode = new EnumModeSetting<>("Animation", AnimationMode.SCALE_IN, AnimationMode.values());
    private final BooleanSetting importantModules = new BooleanSetting("Important", false);

    // Visual settings
    private final EnumModeSetting<TextShadowMode> textShadow = new EnumModeSetting<>("Text Shadow", TextShadowMode.BLACK, TextShadowMode.values());
    private final EnumModeSetting<OutlineMode> outlineMode = new EnumModeSetting<>("Outline", OutlineMode.NONE, OutlineMode.values());
    private final BooleanSetting partialGlow = new BooleanSetting("Partial Glow", true);
    private final BooleanSetting minecraftFont = new BooleanSetting("Minecraft Font", false);
    private final DoubleSetting textSpacing = new DoubleSetting("Text Spacing", 10.5, 8.0, 20.0, 0.5);
    private final DoubleSetting scale = new DoubleSetting("Scale", 1.0, 0.5, 2.0, 0.01);
    private final DoubleSetting letterSpacing = new DoubleSetting("Letter Spacing", 0.0, 0.0, 3.0, 0.1);
    private final BooleanSetting lowercase = new BooleanSetting("Lowercase", false);

    // Background settings
    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final BooleanSetting backgroundColor = new BooleanSetting("Background Color", false);
    private final DoubleSetting backgroundAlpha = new DoubleSetting("Background Alpha", 0.35, 0.0, 1.0, 0.01);
    private final BooleanSetting roundedCorners = new BooleanSetting("Rounded Corners", true);
    private final BooleanSetting gradientBackground = new BooleanSetting("Gradient BG", false);

    // Color settings
    private final EnumModeSetting<ColorMode> colorMode = new EnumModeSetting<>("Color Mode", ColorMode.GRADIENT, ColorMode.values());
    private final EnumModeSetting<Theme> theme = new EnumModeSetting<>("Theme", Theme.SUNSET, Theme.values());
    private final IntegerSetting colorSpeed = new IntegerSetting("Color Speed", 15, 2, 60, 1);

    // Custom colors
    private final IntegerSetting color1R = new IntegerSetting("Color1 R", 255, 0, 255, 1);
    private final IntegerSetting color1G = new IntegerSetting("Color1 G", 100, 0, 255, 1);
    private final IntegerSetting color1B = new IntegerSetting("Color1 B", 100, 0, 255, 1);
    private final IntegerSetting color2R = new IntegerSetting("Color2 R", 255, 0, 255, 1);
    private final IntegerSetting color2G = new IntegerSetting("Color2 G", 200, 0, 255, 1);
    private final IntegerSetting color2B = new IntegerSetting("Color2 B", 50, 0, 255, 1);
    private final IntegerSetting color3R = new IntegerSetting("Color3 R", 100, 0, 255, 1);
    private final IntegerSetting color3G = new IntegerSetting("Color3 G", 150, 0, 255, 1);
    private final IntegerSetting color3B = new IntegerSetting("Color3 B", 255, 0, 255, 1);
    private final IntegerSetting color4R = new IntegerSetting("Color4 R", 180, 0, 255, 1);
    private final IntegerSetting color4G = new IntegerSetting("Color4 G", 80, 0, 255, 1);
    private final IntegerSetting color4B = new IntegerSetting("Color4 B", 255, 0, 255, 1);

    // Animation effects
    private final DoubleSetting pulseIntensity = new DoubleSetting("Pulse Intensity", 0.18, 0.0, 0.8, 0.01);
    private final DoubleSetting swirlStrength = new DoubleSetting("Swirl Strength", 0.12, 0.0, 0.5, 0.01);

    // Glow settings
    private final BooleanSetting glowEnabled = new BooleanSetting("Name Glow", true);
    private final DoubleSetting glowRadius = new DoubleSetting("Glow Radius", 6.0, 0.5, 36.0, 0.5);
    private final DoubleSetting glowAlpha = new DoubleSetting("Glow Alpha", 0.16, 0.0, 1.0, 0.01);
    private final IntegerSetting glowColorR = new IntegerSetting("Glow R", 255, 0, 255, 1);
    private final IntegerSetting glowColorG = new IntegerSetting("Glow G", 140, 0, 255, 1);
    private final IntegerSetting glowColorB = new IntegerSetting("Glow B", 90, 0, 255, 1);

    // Adjust-specific settings
    private final BooleanSetting showModuleInfo = new BooleanSetting("Show Module Info", true);
    private final IntegerSetting xOffset = new IntegerSetting("X-Offset", 1, -120, 50, 1);
    private final IntegerSetting yOffset = new IntegerSetting("Y-Offset", 1, -500, 50, 1);

    private VestigeFontRenderer font;
    private ClientTheme themeModule;
    private long startTime = System.currentTimeMillis();
    private int lastCount;

    public ModuleList() {
        super("Module List", Category.VISUAL, 5, 5, 100, 200, AlignType.RIGHT);

        this.addSettings(
                adjustMode,
                alignMode, animSpeed, animationMode, importantModules,
                textShadow, outlineMode, partialGlow, minecraftFont,
                textSpacing, scale, letterSpacing, lowercase,
                background, backgroundColor, backgroundAlpha, roundedCorners, gradientBackground,
                colorMode, theme, colorSpeed,
                color1R, color1G, color1B,
                color2R, color2G, color2B,
                color3R, color3G, color3B,
                color4R, color4G, color4B,
                pulseIntensity, swirlStrength,
                glowEnabled, glowRadius, glowAlpha, glowColorR, glowColorG, glowColorB,
                showModuleInfo, xOffset, yOffset
        );

        this.listenType = EventListenType.MANUAL;
        this.startListening();
        this.setEnabledSilently(true);
    }

    @Override
    public void onClientStarted() {
        Vestige.instance.getModuleManager().modules.forEach(m -> modules.add(new AnimationHolder<>(m)));
        font = Vestige.instance.getFontManager().getComfortaa();
        themeModule = Vestige.instance.getModuleManager().getModule(ClientTheme.class);
        initialised = false;
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void renderModule(boolean inChat) {
        if (!initialised) {
            sortModules();
            initialised = true;
        }

        if (mc.gameSettings.showDebugInfo) return;

        alignType = alignMode.getMode();
        renderUnifiedList();
    }

    @Listener
    public void onPostMotion(PostMotionEvent e) {
        sortModules();
    }

    private void sortModules() {
        modules.sort((a, b) -> {
            String s1 = getDisplayText(a.get());
            String s2 = getDisplayText(b.get());
            float w1 = getTextWidth(s1);
            float w2 = getTextWidth(s2);
            return Float.compare(w2, w1);
        });
    }

    private String getDisplayText(Module m) {
        String name = lowercase.isEnabled() ? m.getName().toLowerCase() : m.getName();
        String suffix = getSuffix(m);
        return name + suffix;
    }

    private String getSuffix(Module m) {
        if (adjustMode.isEnabled() && !showModuleInfo.isEnabled()) return "";
        String s = m.getSuffix();
        return (s != null && !s.isEmpty()) ? " " + s : "";
    }

    private float getTextWidth(String text) {
        float baseWidth = (float) getCurrentFont().getStringWidth(text);
        float spacing = (float) letterSpacing.getValue();
        if (spacing > 0 && text.length() > 1) {
            baseWidth += spacing * (text.length() - 1);
        }
        return baseWidth * (float) scale.getValue();
    }

    private VestigeFontRenderer getCurrentFont() {
        if (adjustMode.isEnabled()) {
            return Vestige.instance.getFontManager().getProductSans();
        }
        return minecraftFont.isEnabled() ?
                Vestige.instance.getFontManager().getProductSans() :
                Vestige.instance.getFontManager().getComfortaa();
    }

    private void renderUnifiedList() {
        font = getCurrentFont();
        ScaledResolution sr = new ScaledResolution(mc);

        float baseX = adjustMode.isEnabled() ? (float) xOffset.getValue() : (float) posX.getValue();
        float baseY = adjustMode.isEnabled() ? (float) yOffset.getValue() : (float) posY.getValue();
        float offsetY = (float) textSpacing.getValue();
        float scaleValue = (float) scale.getValue();

        applyThemeIfNeeded();

        ArrayList<AnimationHolder<Module>> visible = new ArrayList<>();

        for (AnimationHolder<Module> h : modules) {
            Module m = h.get();
            if (!m.isEnabled()) continue;
            if (importantModules.isEnabled() && m.getCategory() == Category.VISUAL) continue;

            h.setAnimType(AnimationType.SLIDE);
            h.setAnimDuration(animSpeed.getValue());
            h.updateState(m.isEnabled());

            if (!h.isAnimDone() && !h.isRendered()) continue;
            visible.add(h);
        }

        if (visible.isEmpty()) {
            this.height = 0;
            return;
        }

        lastCount = visible.size();
        Color[] colors = getCurrentColors();
        long now = System.currentTimeMillis();
        float timeSeconds = (float) (now - startTime) / 1000.0f;

        if (gradientBackground.isEnabled() && background.isEnabled()) {
            drawGradientBackground(visible, baseX, baseY, offsetY, colors, sr, scaleValue);
        }

        float yPos = baseY;
        int count = 0;

        for (AnimationHolder<Module> holder : visible) {
            Module m = holder.get();
            String displayText = getDisplayText(m);
            String name = lowercase.isEnabled() ? m.getName().toLowerCase() : m.getName();
            String suffix = getSuffix(m);

            float textWidth = getTextWidth(displayText);
            float drawWidth = textWidth + 6f;
            float mult = holder.getYMult();

            float xValue = adjustMode.isEnabled() ?
                    sr.getScaledWidth() - baseX :
                    sr.getScaledWidth() - baseX;
            boolean flip = xValue <= sr.getScaledWidth() / 2f;
            float baseDrawX = adjustMode.isEnabled() ?
                    xValue - textWidth :
                    (flip ? xValue : sr.getScaledWidth() - (drawWidth + baseX));

            GlStateManager.pushMatrix();

            if (scaleValue != 1.0f) {
                float centerX = baseDrawX + textWidth / 2f;
                float centerY = yPos + (font.getHeight() * scaleValue) / 2f;
                GlStateManager.translate(centerX, centerY, 0);
                GlStateManager.scale(scaleValue, scaleValue, 1.0f);
                GlStateManager.translate(-centerX, -centerY, 0);
            }

            if (animationMode.getMode() == AnimationMode.SCALE_IN && !holder.isAnimDone()) {
                float scaleX = baseDrawX + textWidth / 2f;
                float scaleY = yPos + (font.getHeight() * scaleValue) / 2f;
                GlStateManager.translate(scaleX, scaleY, 0);
                GlStateManager.scale(mult, mult, 1.0f);
                GlStateManager.translate(-scaleX, -scaleY, 0);
            }

            final int finalCount = count;
            final float finalTextWidth = textWidth;
            final float finalY = yPos;
            final boolean finalFlip = flip;
            final String finalName = name;
            final String finalSuffix = suffix;

            holder.render(() -> {
                float currentX = baseDrawX;
                float currentAlpha = mult;

                if (animationMode.getMode() == AnimationMode.MOVE_IN) {
                    if (finalFlip) {
                        currentX -= Math.abs((mult - 1) * (sr.getScaledWidth() - (baseX + finalTextWidth)));
                    } else {
                        currentX += Math.abs((mult - 1) * (baseX + finalTextWidth));
                    }
                    currentAlpha = 1.0f;
                }

                if (background.isEnabled() && !gradientBackground.isEnabled()) {
                    drawBackground(currentX, finalY, finalTextWidth, font.getHeight() * scaleValue, colors, currentAlpha);
                }

                if (outlineMode.getMode() != OutlineMode.NONE) {
                    drawOutline(currentX, finalY, finalTextWidth, font.getHeight() * scaleValue,
                            colors[0], finalCount, lastCount, currentAlpha, finalFlip);
                }

                if (glowEnabled.isEnabled()) {
                    drawGlow(currentX, finalY, finalTextWidth, font.getHeight() * scaleValue);
                }

                float textY = finalY + (font.getHeight() * scaleValue) / 2f - 2f;
                if (colorMode.getMode() == ColorMode.GRADIENT) {
                    drawGradientText(finalName, finalSuffix, currentX, textY, colors, currentAlpha,
                            finalCount, timeSeconds);
                } else {
                    drawStaticText(finalName, finalSuffix, currentX, textY, colors[0], currentAlpha);
                }

            }, baseDrawX, yPos, baseDrawX + drawWidth, yPos + offsetY * mult);

            GlStateManager.popMatrix();

            yPos += offsetY * Math.min(mult * 4.0f, 1.0f) * scaleValue;
            count++;
        }

        this.height = (int) ((yPos - baseY) * scaleValue);
    }

    private void drawBackground(float x, float y, float width, float height, Color[] colors, float alpha) {
        Color bgColor;
        if (backgroundColor.isEnabled()) {
            bgColor = darkerColor(colors[0], 0.12f);
        } else {
            bgColor = new Color(10, 10, 10);
        }

        int bgAlpha = (int) (backgroundAlpha.getValue() * 255.0f * alpha);

        if (roundedCorners.isEnabled()) {
            drawRoundedRect(x - 2f, y - 1f, x + width + 3f, y + height + 1f,
                    applyOpacity(bgColor.getRGB(), (float) (bgAlpha / 255.0f)), 3.0f);
        } else {
            drawRect(x - 2f, y - 1f, x + width + 3f, y + height + 1f,
                    applyOpacity(bgColor.getRGB(), (float) (bgAlpha / 255.0f)));
        }
    }

    private void drawOutline(float x, float y, float width, float height, Color color,
                             int index, int total, float alpha, boolean flip) {
        int outlineColor = partialGlow.isEnabled() ?
                applyOpacity(color.getRGB(), alpha) :
                applyOpacity(Color.WHITE.getRGB(), alpha);

        switch (outlineMode.getMode()) {
            case TOP:
                if (index == 0) {
                    drawRect(x - 2f, y - 1f, x + width + 3f, y, outlineColor);
                }
                break;
            case SIDE:
                if (flip) {
                    drawRect(x - 3f, y, x - 2f, y + height, outlineColor);
                } else {
                    drawRect(x + width + 2f, y, x + width + 3f, y + height, outlineColor);
                }
                break;
            case FULL:
                drawRect(x + width + 2f, y, x + width + 3f, y + height, outlineColor);
                drawRect(x - 3f, y, x - 2f, y + height, outlineColor);
                if (index == 0) {
                    drawRect(x - 3f, y - 1f, x + width + 3f, y, outlineColor);
                }
                if (index == total - 1) {
                    drawRect(x - 3f, y + height, x + width + 3f, y + height + 1f, outlineColor);
                }
                if (index < total - 1) {
                    drawRect(x - 3f, y + height, x - 2f, y + height + 1f, outlineColor);
                }
                break;
        }
    }

    private void drawGradientBackground(ArrayList<AnimationHolder<Module>> visible, float baseX,
                                        float baseY, float offsetY, Color[] colors,
                                        ScaledResolution sr, float scale) {
        if (visible.isEmpty()) return;

        float maxWidth = 0;
        float totalHeight = 0;

        for (AnimationHolder<Module> holder : visible) {
            Module m = holder.get();
            String text = getDisplayText(m);
            float width = getTextWidth(text) + 6f;
            if (width > maxWidth) maxWidth = width;
            totalHeight += offsetY * scale;
        }

        float x = sr.getScaledWidth() - baseX - maxWidth - 2f;
        float y = baseY - 1f;
        float width = maxWidth + 4f;
        float height = totalHeight + 2f;

        Color startColor = colors[0];
        Color endColor = colors[colors.length > 1 ? 1 : 0];

        int alpha = (int) (backgroundAlpha.getValue() * 255);
        startColor = new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), alpha);
        endColor = new Color(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), alpha);

        drawGradientRect(x, y, x + width, y + height, startColor.getRGB(), endColor.getRGB());
    }

    private void drawGlow(float x, float y, float width, float height) {
        float radius = (float) glowRadius.getValue();
        float alphaVal = (float) glowAlpha.getValue();
        int glowR = glowColorR.getValue();
        int glowG = glowColorG.getValue();
        int glowB = glowColorB.getValue();
        Color glowColor = new Color(glowR, glowG, glowB);

        drawSoftGlow(x, y - 1f, width, height + 2f, glowColor, alphaVal, radius);
    }

    private void drawSoftGlow(float x, float y, float width, float height, Color color,
                              float intensity, float radius) {
        radius = Math.max(0.5f, radius);
        intensity = Math.max(0f, Math.min(1f, intensity));
        int layers = Math.max(3, (int) Math.min(24, Math.round(radius / 1.5f)));

        for (int i = layers; i >= 1; i--) {
            float t = i / (float) layers;
            float spread = radius * t;
            float a = intensity * (t * 0.8f);
            int col = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    Math.max(0, Math.min(255, (int) (a * 255f)))).getRGB();

            float lx = x - spread;
            float ly = y - spread;
            float lw = width + spread * 2f;
            float lh = height + spread * 2f;

            if (roundedCorners.isEnabled()) {
                drawRoundedRect(lx, ly, lx + lw, ly + lh, col, Math.max(1.0f, spread * 0.4f));
            } else {
                drawRect(lx, ly, lx + lw, ly + lh, col);
            }
        }
    }

    private void drawGradientText(String name, String suffix, float x, float y, Color[] colors,
                                  float alpha, int moduleIndex, float timeSeconds) {
        float nameWidth = drawTextWithGradient(name, x, y, colors, alpha, moduleIndex, timeSeconds);

        if (!suffix.isEmpty()) {
            drawTextWithShadow(suffix, x + nameWidth, y, new Color(200, 200, 200), alpha);
        }
    }

    private float drawTextWithGradient(String text, float x, float y, Color[] colors, float alpha,
                                       int moduleIndex, float timeSeconds) {
        if (text == null || text.isEmpty()) return 0;

        float pulseVal = (float) pulseIntensity.getValue();
        float swirlVal = (float) swirlStrength.getValue();
        float globalPulse = (0.5f + 0.5f * (float) Math.sin(timeSeconds * 2.0f)) * pulseVal + (1f - pulseVal);

        float cycleTime = Math.max(1f, colorSpeed.getValue()) * 1000f;
        float baseProgress = ((float) (System.currentTimeMillis() % (long) cycleTime)) / cycleTime;

        float cx = x;
        float spacing = (float) letterSpacing.getValue();

        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float cw = (float) font.getStringWidth(ch);

            float rowNormalized = ((moduleIndex * 1.0f) / Math.max(1f, lastCount)) + baseProgress * 0.25f;
            rowNormalized = rowNormalized % 1.0f;

            float swirlOffset = (float) Math.sin((cx * 0.02f) + (timeSeconds * 0.6f) + moduleIndex * 0.12f) * swirlVal;
            float samplePos = (rowNormalized + swirlOffset) % 1.0f;
            if (samplePos < 0f) samplePos += 1f;

            Color base = getSmoothLoopingColor(colors, samplePos);

            int r = (int) Math.max(0f, Math.min(255f, base.getRed() * globalPulse));
            int g = (int) Math.max(0f, Math.min(255f, base.getGreen() * globalPulse));
            int b = (int) Math.max(0f, Math.min(255f, base.getBlue() * globalPulse));
            Color finalColor = new Color(r, g, b, base.getAlpha());

            drawCharacterWithShadow(ch, cx, y, applyOpacity(finalColor.getRGB(), alpha), alpha);

            cx += cw + spacing;
        }

        return cx - x;
    }

    private void drawStaticText(String name, String suffix, float x, float y, Color color, float alpha) {
        float nameWidth = drawTextWithSpacing(name, x, y, color, alpha);

        if (!suffix.isEmpty()) {
            drawTextWithShadow(suffix, x + nameWidth, y, new Color(200, 200, 200), alpha);
        }
    }

    private float drawTextWithSpacing(String text, float x, float y, Color color, float alpha) {
        if (text == null || text.isEmpty()) return 0;

        float cx = x;
        float spacing = (float) letterSpacing.getValue();
        int finalColor = applyOpacity(color.getRGB(), alpha);

        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float cw = (float) font.getStringWidth(ch);

            drawCharacterWithShadow(ch, cx, y, finalColor, alpha);

            cx += cw + spacing;
        }

        return cx - x;
    }

    private void drawCharacterWithShadow(String character, float x, float y, int color, float alpha) {
        switch (textShadow.getMode()) {
            case NONE:
                font.drawString(character, x, y, color);
                break;
            case COLORED:
                int shadowColor = darker(color, 0.5f);
                font.drawString(StringUtils.stripControlCodes(character), x + 1f, y + 1f, shadowColor);
                font.drawString(character, x, y, color);
                break;
            case BLACK:
                float offset = minecraftFont.isEnabled() ? 1f : 0.5f;
                font.drawString(StringUtils.stripControlCodes(character), x + offset, y + offset,
                        applyOpacity(Color.BLACK.getRGB(), alpha));
                font.drawString(character, x, y, color);
                break;
        }
    }

    private void drawTextWithShadow(String text, float x, float y, Color color, float alpha) {
        int finalColor = applyOpacity(color.getRGB(), alpha);

        switch (textShadow.getMode()) {
            case NONE:
                font.drawString(text, x, y, finalColor);
                break;
            case COLORED:
                int shadowColor = darker(color.getRGB(), 0.5f);
                font.drawString(StringUtils.stripControlCodes(text), x + 1f, y + 1f, shadowColor);
                font.drawString(text, x, y, finalColor);
                break;
            case BLACK:
                float offset = minecraftFont.isEnabled() ? 1f : 0.5f;
                font.drawString(StringUtils.stripControlCodes(text), x + offset, y + offset,
                        applyOpacity(Color.BLACK.getRGB(), alpha));
                font.drawString(text, x, y, finalColor);
                break;
        }
    }

    private Color[] getCurrentColors() {
        if (theme.getMode() != Theme.CUSTOM) {
            return theme.getMode().colors;
        }
        return new Color[] {
                new Color(color1R.getValue(), color1G.getValue(), color1B.getValue()),
                new Color(color2R.getValue(), color2G.getValue(), color2B.getValue()),
                new Color(color3R.getValue(), color3G.getValue(), color3B.getValue()),
                new Color(color4R.getValue(), color4G.getValue(), color4B.getValue())
        };
    }

    private Color getSmoothLoopingColor(Color[] colors, float progress) {
        if (colors == null || colors.length == 0) return Color.WHITE;
        if (colors.length == 1) return colors[0];

        float scaled = progress * colors.length;
        int idx = (int) Math.floor(scaled) % colors.length;
        int next = (idx + 1) % colors.length;
        float local = scaled - (float) Math.floor(scaled);

        Color a = colors[idx];
        Color b = colors[next];
        int r = (int) (a.getRed() * (1f - local) + b.getRed() * local);
        int g = (int) (a.getGreen() * (1f - local) + b.getGreen() * local);
        int bl = (int) (a.getBlue() * (1f - local) + b.getBlue() * local);
        int alpha = (int) (a.getAlpha() * (1f - local) + b.getAlpha() * local);

        return new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, bl)),
                Math.max(0, Math.min(255, alpha))
        );
    }

    private void applyThemeIfNeeded() {
        Theme sel = theme.getMode();
        if (sel == Theme.CUSTOM) return;

        Color[] themeColors = sel.colors;
        if (themeColors.length >= 1) {
            color1R.setValue(themeColors[0].getRed());
            color1G.setValue(themeColors[0].getGreen());
            color1B.setValue(themeColors[0].getBlue());
        }
        if (themeColors.length >= 2) {
            color2R.setValue(themeColors[1].getRed());
            color2G.setValue(themeColors[1].getGreen());
            color2B.setValue(themeColors[1].getBlue());
        }
        if (themeColors.length >= 3) {
            color3R.setValue(themeColors[2].getRed());
            color3G.setValue(themeColors[2].getGreen());
            color3B.setValue(themeColors[2].getBlue());
        }
        if (themeColors.length >= 4) {
            color4R.setValue(themeColors[3].getRed());
            color4G.setValue(themeColors[3].getGreen());
            color4B.setValue(themeColors[3].getBlue());
        }
    }

    private void drawRoundedRect(float left, float top, float right, float bottom, int color, float radius) {
        drawRect(left + radius, top, right - radius, bottom, color);
        drawRect(left, top + radius, right, bottom - radius, color);
        drawRect(left + radius, top + radius, right - radius, bottom - radius, color);
    }

    private void drawGradientRect(float left, float top, float right, float bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, top, 0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, bottom, 0).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(right, bottom, 0).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private void drawRect(float left, float top, float right, float bottom, int color) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        GlStateManager.color(r, g, b, a);
        wr.begin(7, DefaultVertexFormats.POSITION);
        wr.pos(left, bottom, 0).endVertex();
        wr.pos(right, bottom, 0).endVertex();
        wr.pos(right, top, 0).endVertex();
        wr.pos(left, top, 0).endVertex();
        tess.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private int applyOpacity(int color, float opacity) {
        int alpha = (int) ((color >> 24 & 0xFF) * opacity);
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    private int darker(int color, float factor) {
        int r = (int) ((color >> 16 & 0xFF) * factor);
        int g = (int) ((color >> 8 & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private Color darkerColor(Color c, float factor) {
        int r = Math.max(0, Math.min(255, (int) (c.getRed() * (1f - factor))));
        int g = Math.max(0, Math.min(255, (int) (c.getGreen() * (1f - factor))));
        int b = Math.max(0, Math.min(255, (int) (c.getBlue() * (1f - factor))));
        return new Color(r, g, b, c.getAlpha());
    }

    public enum AnimationMode {
        MOVE_IN("Move in"), SCALE_IN("Scale in");
        private final String name;
        AnimationMode(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public enum TextShadowMode {
        COLORED("Colored"), BLACK("Black"), NONE("None");
        private final String name;
        TextShadowMode(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public enum OutlineMode {
        NONE("None"), TOP("Top"), SIDE("Side"), FULL("Full");
        private final String name;
        OutlineMode(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public enum ColorMode {
        STATIC("Static"), GRADIENT("Gradient");
        private final String name;
        ColorMode(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public enum Theme {
        CUSTOM(new Color[]{
                new Color(255, 100, 100),
                new Color(255, 200, 50),
                new Color(255, 100, 200),
                new Color(255, 50, 100)
        }),
        SUNSET(new Color[]{
                new Color(255, 140, 80),
                new Color(255, 100, 150),
                new Color(255, 180, 90),
                new Color(200, 100, 220)
        }),
        TENACITY(new Color[]{
                new Color(255, 126, 95),
                new Color(254, 180, 123),
                new Color(255, 215, 120),
                new Color(200, 110, 230)
        }),
        ASTOLFO(new Color[]{
                new Color(255, 160, 220),
                new Color(200, 120, 255),
                new Color(255, 130, 180),
                new Color(255, 200, 230)
        }),
        OCEAN(new Color[]{
                new Color(10, 80, 160),
                new Color(20, 140, 200),
                new Color(50, 200, 255),
                new Color(100, 230, 255)
        }),
        AURORA(new Color[]{
                new Color(64, 255, 200),
                new Color(120, 80, 255),
                new Color(255, 100, 150),
                new Color(255, 200, 100)
        });

        public final Color[] colors;
        Theme(Color[] colors) { this.colors = colors; }
    }
}