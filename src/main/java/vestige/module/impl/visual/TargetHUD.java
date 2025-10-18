package vestige.module.impl.visual;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import vestige.Vestige;
import vestige.module.AlignType;
import vestige.module.Category;
import vestige.module.HUDModule;
import vestige.module.impl.combat.Killaura;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.IntegerSetting;
import vestige.setting.impl.ModeSetting;
import vestige.util.misc.TimerUtil;
import vestige.util.render.DrawUtil;
import vestige.util.render.FontUtil;
import vestige.util.render.RenderUtil;
import vestige.util.render.BlurUtil;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TargetHUD extends HUDModule {

    private final IntegerSetting fadeSpeed = new IntegerSetting("Fade speed", 200, 50, 500, 25);
    private final IntegerSetting healthBarDelay = new IntegerSetting("Health bar delay", 100, 0, 450, 25);
    private final BooleanSetting roundedHealth = new BooleanSetting("Rounded health", true);
    private final BooleanSetting dynamicHealthColor = new BooleanSetting("Dynamic Health Color", true);
    private final BooleanSetting blur = new BooleanSetting("Blur", true);

    private final BooleanSetting shadow = new BooleanSetting("Shadow", true);
    private final IntegerSetting shadowOpacity = new IntegerSetting("Shadow Opacity", 80, 0, 255, 5);

    private final ModeSetting font = FontUtil.getFontSetting();

    private final IntegerSetting bgR = new IntegerSetting("Background Red", 45, 0, 255, 1);
    private final IntegerSetting bgG = new IntegerSetting("Background Green", 45, 0, 255, 1);
    private final IntegerSetting bgB = new IntegerSetting("Background Blue", 45, 0, 255, 1);

    private final IntegerSetting outlineR = new IntegerSetting("Outline Red", 100, 0, 255, 1);
    private final IntegerSetting outlineG = new IntegerSetting("Outline Green", 255, 0, 255, 1);
    private final IntegerSetting outlineB = new IntegerSetting("Outline Blue", 100, 0, 255, 1);

    private final IntegerSetting healthBarR = new IntegerSetting("Health Bar Red", 255, 154, 0, 1);
    private final IntegerSetting healthBarG = new IntegerSetting("Health Bar Green", 99, 0, 255, 1);
    private final IntegerSetting healthBarB = new IntegerSetting("Health Bar Blue", 164, 0, 255, 1);

    private final ModeSetting style = new ModeSetting("Style", "Modern", "Modern", "Astolfo", "Rise", "Tenacity", "Adjust");

    private final IntegerSetting riseParticleCount = new IntegerSetting("Rise Particles", 12, 0, 60, 1);
    private final IntegerSetting riseParticleMaxSize = new IntegerSetting("Rise Particle Size", 4, 1, 12, 1);

    private final IntegerSetting astolfoAccentR = new IntegerSetting("Astolfo Accent R", 255, 0, 255, 1);
    private final IntegerSetting astolfoAccentG = new IntegerSetting("Astolfo Accent G", 100, 0, 255, 1);
    private final IntegerSetting astolfoAccentB = new IntegerSetting("Astolfo Accent B", 200, 0, 255, 1);

    private final IntegerSetting tenacitySwirlSpeed = new IntegerSetting("Tenacity Swirl Speed", 2500, 500, 8000, 100);
    private final IntegerSetting tenacitySwirlStrength = new IntegerSetting("Tenacity Strength", 80, 0, 255, 1);
    private final BooleanSetting tenacityUseGradient = new BooleanSetting("Tenacity Animated Gradient", true);

    private final BooleanSetting adjustShowAvatar = new BooleanSetting("Adjust Show Avatar", true);
    private final BooleanSetting adjustFollowPlayer = new BooleanSetting("Adjust Follow Player", false);
    private final IntegerSetting adjustBackgroundAlpha = new IntegerSetting("Adjust BG Alpha", 180, 0, 255, 1);
    private final IntegerSetting adjustGhostBarDuration = new IntegerSetting("Adjust Ghost Duration", 30, 0, 350, 1);
    private final IntegerSetting adjustGhostBarAlpha = new IntegerSetting("Adjust Ghost Alpha", 100, 0, 255, 1);
    private final ModeSetting adjustColorMode = new ModeSetting("Adjust Color Mode", "Cherry",
            "Cherry", "Cotton Candy", "Flare", "Flower", "Gold", "Grayscale", "Royal", "Sky", "Vine",
            "Red", "Blue", "Idk", "Idk2", "Opal", "Candy Cane", "Idklol", "What", "Sunset");
    private final IntegerSetting adjustDamageAlphaSetting = new IntegerSetting("Adjust Damage Alpha", 128, 0, 255, 1);
    private final IntegerSetting adjustDamageDuration = new IntegerSetting("Adjust Damage Duration", 128, 0, 255, 1);

    private final IntegerSetting animationMS = new IntegerSetting("Animation (ms)", 300, 50, 2000, 25);

    private enum AnimState { IDLE, OPENING, CLOSING }
    private AnimState animState = AnimState.IDLE;
    private float animProgress = 1.0f;
    private long lastAnimTime = System.currentTimeMillis();

    private float fadeAlpha = 0.0F;
    private float targetAlpha = 0.0F;

    private Killaura killauraModule;
    private EntityPlayer target;
    private final TimerUtil barTimer = new TimerUtil();

    private float renderedHealth;
    private float displayedFill = 1.0f;
    private float trailFill = 1.0f;
    private boolean hadTarget;
    private float pulseAnimation = 0.0F;

    private final List<Particle> riseParticles = new ArrayList<>();
    private boolean riseSentParticles = false;

    private final DecimalFormat DF_1O = new DecimalFormat("0.#");

    private long tenacityStartTime = System.currentTimeMillis();

    private float adjustCurrentHealthBarFillWidth = 0.008f;
    private float adjustLastHealthBarFillWidth = 0.008f;
    private int adjustGhostBarTicks = 0;
    private int adjustDamageTicks = 0;
    private float adjustLastTargetHealth = -1;
    private int adjustBgAlphaValue, adjustGhostAlphaValue, adjustDamageAlphaValue;
    private int adjustFadeOutTicks = 0;
    private final int adjustMaxFadeOutTicks = 7;
    private int adjustOriginalBackgroundAlpha, adjustOriginalGhostBarAlpha, adjustOriginalDamageAlpha;
    private boolean adjustFadingOut = false;

    private final Color[][] adjustColorModes = {
            { new Color(255, 200, 200), new Color(243, 58, 106) },
            { new Color(99, 249, 255), new Color(255, 104, 204) },
            { new Color(231, 39, 24), new Color(245, 173, 49) },
            { new Color(215, 166, 231), new Color(211, 90, 232) },
            { new Color(255, 215, 0), new Color(240, 159, 0) },
            { new Color(240, 240, 240), new Color(110, 110, 110) },
            { new Color(125, 204, 241), new Color(30, 71, 170) },
            { new Color(160, 230, 225), new Color(15, 190, 220) },
            { new Color(17, 192, 45), new Color(201, 234, 198) },
            { new Color(234, 64, 24), new Color(161, 228, 49) },
            { new Color(143, 0, 91), new Color(0, 70, 255) },
            { new Color(255, 0, 0), new Color(170, 20, 20) },
            { new Color(0, 158, 255), new Color(15, 255, 73) },
            { new Color(0, 0, 25), new Color(15, 255, 73) },
            { new Color(0, 255, 255), new Color(0, 0, 255) },
            { new Color(255, 255, 255), new Color(255, 0, 0) },
            { new Color(194, 0, 121), new Color(182, 0, 255) },
            { new Color(255, 0, 52), new Color(255, 0, 0) },
    };

    public TargetHUD() {
        super("TargetHUD", Category.VISUAL, 0, 0, 140, 32, AlignType.LEFT);

        this.addSettings(
                font, style, fadeSpeed, healthBarDelay, roundedHealth, dynamicHealthColor, blur,
                shadow, shadowOpacity,
                bgR, bgG, bgB, outlineR, outlineG, outlineB, healthBarR, healthBarG, healthBarB,
                riseParticleCount, riseParticleMaxSize,
                astolfoAccentR, astolfoAccentG, astolfoAccentB,
                tenacitySwirlSpeed, tenacitySwirlStrength, tenacityUseGradient,
                adjustShowAvatar, adjustFollowPlayer, adjustBackgroundAlpha, adjustGhostBarDuration,
                adjustGhostBarAlpha, adjustColorMode, adjustDamageAlphaSetting, adjustDamageDuration,
                animationMS
        );

        ScaledResolution sr = new ScaledResolution(mc);
        this.posX.setValue(sr.getScaledWidth() / 2 - 70);
        this.posY.setValue(sr.getScaledHeight() / 2 + 20);
    }

    @Override
    public void onClientStarted() {
        killauraModule = Vestige.instance.getModuleManager().getModule(Killaura.class);
    }

    @Override
    protected void renderModule(boolean inChat) {
        if (inChat) {
            fadeAlpha = 1.0F;
            targetAlpha = 1.0F;
            renderTargetHUD(mc.thePlayer);
            target = null;
        } else if (this.isEnabled()) {
            boolean canRender = killauraModule != null && killauraModule.isEnabled() && killauraModule.getTarget() instanceof EntityPlayer;
            if (canRender) {
                target = (EntityPlayer) killauraModule.getTarget();
                targetAlpha = 1.0F;
            } else {
                targetAlpha = 0.0F;
            }

            float fadeStep = 0.016F / (fadeSpeed.getValue() / 1000.0F);
            fadeAlpha = targetAlpha > fadeAlpha ? Math.min(fadeAlpha + fadeStep, targetAlpha) : Math.max(fadeAlpha - fadeStep, targetAlpha);

            boolean desiredVisible = targetAlpha > 0.0001f;
            long now = System.currentTimeMillis();
            long dt = Math.max(1L, now - lastAnimTime);
            lastAnimTime = now;
            float step = (float) dt / (float) Math.max(1, animationMS.getValue());

            if (desiredVisible) {
                if (animState != AnimState.OPENING) animState = AnimState.OPENING;
                animProgress += step;
                if (animProgress > 1.0f) animProgress = 1.0f;
            } else {
                if (animState != AnimState.CLOSING) animState = AnimState.CLOSING;
                animProgress -= step;
                if (animProgress < 0.0f) animProgress = 0.0f;
            }

            float scale;
            if (animState == AnimState.OPENING) {
                scale = easeOutCubic(animProgress) * 1.0f;
            } else if (animState == AnimState.CLOSING) {
                float overshoot = 1.0f + 0.1f * (1.0f - animProgress);
                scale = animProgress * overshoot;
            } else {
                scale = animProgress;
            }

            float alphaMult = fadeAlpha * animProgress;
            if (alphaMult < 0f) alphaMult = 0f;
            if (alphaMult > 1f) alphaMult = 1f;

            float cx = (float) posX.getValue() + (float) (this.width) / 2.0f;
            float cy = (float) posY.getValue() + (float) (this.height) / 2.0f;

            GL11.glPushMatrix();
            GL11.glTranslatef(cx, cy, 0.0f);
            GL11.glScalef(scale, scale, 1.0f);
            GL11.glTranslatef(-cx, -cy, 0.0f);

            float prevFade = fadeAlpha;
            fadeAlpha = alphaMult;
            renderTargetHUD(target);
            fadeAlpha = prevFade;

            GL11.glPopMatrix();

        } else {
            fadeAlpha = 0.0F;
            targetAlpha = 0.0F;
        }
    }

    private void renderShadow(float x, float y, float width, float height, float cornerRadius) {
        if (!shadow.isEnabled() || shadowOpacity.getValue() <= 0) return;

        int shadowAlpha = (int) (shadowOpacity.getValue() * fadeAlpha);
        if (shadowAlpha <= 0) return;

        for (int i = 0; i < 3; i++) {
            float offset = i * 0.8f;
            float alpha = shadowAlpha * (1.0f - (i * 0.3f));
            Color layerColor = new Color(0, 0, 0, (int) alpha);

            RenderUtil.drawRoundedRect(
                    x - offset, y - offset,
                    width + offset * 2, height + offset * 2,
                    cornerRadius + offset,
                    layerColor.getRGB()
            );
        }
    }

    private void renderTargetHUD(EntityPlayer entity) {
        if (entity == null) return;

        String s = style.getMode();
        if ("Astolfo".equalsIgnoreCase(s)) {
            renderAstolfoHUD(entity);
        } else if ("Rise".equalsIgnoreCase(s)) {
            renderRiseHUD(entity);
        } else if ("Tenacity".equalsIgnoreCase(s)) {
            renderTenacityHUD(entity);
        } else if ("Adjust".equalsIgnoreCase(s)) {
            renderAdjustHUD(entity);
        } else {
            renderModernHUD(entity);
        }
    }

    private void renderModernHUD(EntityPlayer entity) {
        if (entity == null) return;

        float x = (float) posX.getValue();
        float y = (float) posY.getValue();

        float health = roundedHealth.isEnabled() ? Math.round(entity.getHealth() * 10f) / 10.0f : entity.getHealth();

        if (!hadTarget) {
            renderedHealth = health;
            float initMult = renderedHealth / Math.max(1.0f, entity.getMaxHealth());
            displayedFill = initMult;
            trailFill = initMult;
            hadTarget = true;
        }

        if (health != renderedHealth) {
            float elapsed = (float) barTimer.getTimeElapsed();
            float ratio = Math.min(1.0f, elapsed / (float) Math.max(1, healthBarDelay.getValue()));
            renderedHealth += (health - renderedHealth) * ratio;
        } else {
            barTimer.reset();
        }

        float targetFill = Math.max(0f, Math.min(1f, health / Math.max(1.0f, entity.getMaxHealth())));

        if (targetFill < displayedFill) displayedFill += (targetFill - displayedFill) * 0.06f;
        else displayedFill += (targetFill - displayedFill) * 0.18f;

        if (trailFill > displayedFill) trailFill += (displayedFill - trailFill) * 0.04f;
        else trailFill += (displayedFill - trailFill) * 0.22f;

        displayedFill = Math.max(0f, Math.min(1f, displayedFill));
        trailFill = Math.max(0f, Math.min(1f, trailFill));

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        pulseAnimation += 0.05F;
        if (pulseAnimation > 360.0F) pulseAnimation = 0.0F;
        float pulse = (float) (Math.sin(pulseAnimation) * 0.2D + 0.8D);

        renderShadow(x, y, 140.0F, 32.0F, 4.0F);

        if (blur.isEnabled()) {
            float blurStrength = 8.0F * fadeAlpha;
            BlurUtil.blurArea(x, y, 140.0F, 32.0F, blurStrength);
        }

        int glassAlpha = (int) (30 * fadeAlpha);
        RenderUtil.drawRoundedRect(x, y, 140.0F, 32.0F, 4.0F, new Color(255, 255, 255, glassAlpha).getRGB());

        int glowAlpha = (int) (95 * fadeAlpha * pulse);
        RenderUtil.drawRoundedRect(x - 2.0F, y - 2.0F, 144.0F, 36.0F, 6.0F,
                new Color(outlineR.getValue(), outlineG.getValue(), outlineB.getValue(), glowAlpha).getRGB());

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        RenderUtil.drawRoundedRect(x + 5.0F, y + 4.0F, 24.0F, 24.0F, 12.0F, 0xFFFFFFFF);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        DrawUtil.drawHead(((AbstractClientPlayer) entity).getLocationSkin(), (int) (x + 5.0F), (int) (y + 4.0F), 24, 24);
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        int textAlpha = (int) (245 * fadeAlpha);
        String name = entity.getGameProfile().getName();
        FontUtil.drawStringWithShadow(font.getMode(), name, (int) (x + 35.0F), (int) (y + 4.0F), new Color(255, 255, 255, textAlpha).getRGB());

        float barX = x + 39.0F;
        float barY = y + 22.0F;
        float barWidth = 98.0F;
        float barHeight = 4.0F;
        float realHealthWidth = barWidth;
        float healthWidth = displayedFill * realHealthWidth;

        int barBgAlpha = (int) (140 * fadeAlpha);
        RenderUtil.drawRoundedRect(barX, barY, realHealthWidth, barHeight, barHeight / 2.0F, new Color(12, 12, 12, barBgAlpha).getRGB());

        if (healthWidth > 0.0F) {
            int barAlpha = (int) (230 * fadeAlpha);
            Color leftCol = new Color(healthBarR.getValue(), healthBarG.getValue(), healthBarB.getValue(), barAlpha);
            Color rightCol = new Color(255, 99, 164, barAlpha);
            RenderUtil.drawRoundedGradient(barX, barY, Math.max(1.0F, healthWidth), barHeight, barHeight / 2.0F, leftCol, rightCol);

            int glowBarAlpha = (int) (48 * fadeAlpha * pulse);
            RenderUtil.drawRoundedRect(barX - 0.8F, barY - 0.6F,
                    Math.max(1.0F, healthWidth) + 1.6F, barHeight + 1.2F,
                    barHeight / 2.0F + 0.5F, new Color(255, 80, 150, glowBarAlpha).getRGB());
        }

        float trailWidth = trailFill * realHealthWidth;
        if (trailWidth > healthWidth + 1.0F) {
            RenderUtil.drawRoundedRect(barX + healthWidth, barY, Math.max(0.0F, trailWidth - healthWidth),
                    barHeight, barHeight / 2.0F, new Color(0, 0, 0, (int) (40 * fadeAlpha)).getRGB());
        }

        if (healthWidth > 0.0F) {
            RenderUtil.drawRoundedRect(barX, barY - 0.9F, Math.max(1.0F, healthWidth),
                    barHeight / 2.0F, barHeight / 2.0F, new Color(255, 255, 255, (int) (55 * fadeAlpha)).getRGB());
        }

        int hpPercent = MathHelper.clamp_int((int) (targetFill(health, entity) * 100.0F), 0, 100);
        String healthText = hpPercent + "%";

        float percentX = x + 34.0F + Math.min(Math.max(1.0F, healthWidth), realHealthWidth - 11.0F);
        int percentAlpha = (int) (220 * fadeAlpha);

        FontUtil.drawStringWithShadow(font.getMode(), healthText, (int) percentX, (int) (barY - 9.0F),
                new Color(235, 235, 235, percentAlpha).getRGB());

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GL11.glPopMatrix();

        if (fadeAlpha <= 0.0F) hadTarget = false;
    }

    private void renderAstolfoHUD(EntityPlayer entity) {
        if (entity == null) return;

        float x = (float) posX.getValue();
        float y = (float) posY.getValue();

        float width = Math.max(110f, (float) mc.fontRendererObj.getStringWidth(entity.getName()) + 70f);
        float height = 45f;

        this.width = (int) width;
        this.height = (int) height;

        float alpha = fadeAlpha;
        if (alpha <= 0f) return;

        renderShadow(x, y, width, height, 6.0f);

        if (blur.isEnabled()) {
            float blurStrength = 8.0F * fadeAlpha;
            BlurUtil.blurArea(x, y, width, height, blurStrength);
        }

        Color c1 = new Color(astolfoAccentR.getValue(), astolfoAccentG.getValue(), astolfoAccentB.getValue(), (int) (alpha * 255f));
        Color c2 = new Color(outlineR.getValue(), outlineG.getValue(), outlineB.getValue(), (int) (alpha * 255f));
        int textColor = new Color(255, 255, 255, (int) (alpha * 255f)).getRGB();

        RenderUtil.drawRoundedRect(x, y, width, height, 6.0f, new Color(0, 0, 0, (int) (alpha * 153f)).getRGB());

        float healthPercentage = MathHelper.clamp_float((entity.getHealth()) / (entity.getMaxHealth()), 0f, 1f);
        displayedFill += (healthPercentage - displayedFill) * 0.18f;
        displayedFill = Math.max(0f, Math.min(1f, displayedFill));

        float barLeft = x + 34f;
        float totalBarWidth = width - 38f;
        float healthWidth = displayedFill * totalBarWidth;
        float barTop = y + 33f;
        float barHeight = y + 40f;

        RenderUtil.drawRoundedRect(barLeft, barTop, totalBarWidth, (barHeight - barTop), 3.0f, new Color(0, 0, 0, (int) (alpha * 153f)).getRGB());
        RenderUtil.drawRoundedGradient(barLeft, barTop, Math.max(1.0f, healthWidth), (barHeight - barTop), 3.0f, darker(c1), darker(c2));

        GL11.glColor4f(1f, 1f, 1f, alpha);
        GuiInventory.drawEntityOnScreen((int) x + 17, (int) y + 40, 18, entity.rotationYaw, entity.rotationPitch, entity);

        GL11.glColor4f(1f, 1f, 1f, alpha);
        GL11.glEnable(GL11.GL_BLEND);
        mc.fontRendererObj.drawStringWithShadow(entity.getName(), x + 34f, y + 4f, textColor);

        float scale = 1.75f;
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        mc.fontRendererObj.drawStringWithShadow(DF_1O.format(entity.getHealth()) + " ❤", (x + 34f) / scale, (y + 16f) / scale,
                new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), (int) (alpha * 255f)).getRGB());
        GL11.glPopMatrix();

        RenderUtil.drawRoundedRect(x - 1.5f, y - 1.5f, width + 1.5f, height + 1.5f, 7.0f, new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), (int) (alpha * 80f)).getRGB());

        GL11.glDisable(GL11.GL_BLEND);
    }

    private void renderRiseHUD(EntityPlayer entity) {
        if (entity == null) return;

        float x = (float) posX.getValue();
        float y = (float) posY.getValue();

        float width = Math.max(128f, (float) mc.fontRendererObj.getStringWidth("Name: " + entity.getName()) + 60f);
        float height = 50f;

        this.width = (int) width;
        this.height = (int) height;

        float alpha = fadeAlpha;
        if (alpha <= 0f) return;

        renderShadow(x, y, width, height, 6.0f);

        if (blur.isEnabled()) {
            float blurStrength = 8.0F * fadeAlpha;
            BlurUtil.blurArea(x, y, width, height, blurStrength);
        }

        RenderUtil.drawRoundedRect(x, y, width, height, 6.0f, new Color(0, 0, 0, (int) (110f * alpha)).getRGB());

        float healthPercent = MathHelper.clamp_float((entity.getHealth()) / (entity.getMaxHealth()), 0f, 1f);
        displayedFill += (healthPercent - displayedFill) * 0.18f;
        displayedFill = Math.max(0f, Math.min(1f, displayedFill));

        float barX = x + 5f;
        float barY = y + 40f;
        float totalBarWidth = width - 10f;
        float drawWidth = displayedFill * totalBarWidth;
        float barHeight = 5f;

        int bgAlpha = (int) (120f * alpha);
        RenderUtil.drawRoundedRect(barX, barY, totalBarWidth, barHeight, 0.0f, new Color(10, 10, 10, bgAlpha).getRGB());

        if (drawWidth > 0f) {
            Color c1 = new Color(healthBarR.getValue(), healthBarG.getValue(), healthBarB.getValue(), (int) (alpha * 255f));
            Color c2 = new Color(outlineR.getValue(), outlineG.getValue(), outlineB.getValue(), (int) (alpha * 255f));
            RenderUtil.drawRoundedGradient(barX, barY, Math.max(1.0f, drawWidth), barHeight, 0.0f, c1, c2);
        }

        if (entity instanceof AbstractClientPlayer) {
            float renderX = x + 5f;
            float renderY = y + 5f;
            float size = 30f;

            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

            RenderUtil.drawRoundedRect(renderX, renderY, size, size, size / 2f, 0xFFFFFFFF);
            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

            DrawUtil.drawHead(((AbstractClientPlayer) entity).getLocationSkin(), (int) renderX, (int) renderY, (int) size, (int) size);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }

        int textColor = new Color(255, 255, 255, (int) (alpha * 255f)).getRGB();
        FontUtil.drawStringWithShadow(font.getMode(), "Name: " + entity.getName(), (int) (x + 40f), (int) (y + 10f), textColor);
        FontUtil.drawStringWithShadow(font.getMode(), "Distance: " + Math.round(mc.thePlayer.getDistanceToEntity(entity) * 10f) / 10f,
                (int) (x + 40f), (int) (y + 22f), textColor);

        float currentHealth = entity.getHealth();
        if (currentHealth < renderedHealth - 0.001f && !riseSentParticles) {
            int amount = Math.max(0, Math.min(60, riseParticleCount.getValue()));
            for (int i = 0; i < amount; i++) {
                Particle p = new Particle();
                double angle = Math.random() * Math.PI * 2.0;
                float speed = (float) (Math.random() * 2.0 + 0.8);
                float dx = (float) (Math.cos(angle) * speed);
                float dy = (float) (Math.sin(angle) * speed);
                p.init(x + width / 2f, y + height / 2f, dx, dy,
                        (float) ((Math.random() * (float) riseParticleMaxSize.getValue()) + 1f),
                        new Color(healthBarR.getValue(), healthBarG.getValue(), healthBarB.getValue()));
                riseParticles.add(p);
            }
            riseSentParticles = true;
        }
        if (currentHealth >= renderedHealth - 0.001f) {
            riseSentParticles = false;
        }

        Iterator<Particle> it = riseParticles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.updatePosition();
            if (p.opacity < 1f) {
                it.remove();
                continue;
            }
            RenderUtil.drawRoundedRect(p.x + p.adjustedX, p.y + p.adjustedY, p.size, p.size, Math.max(0.0f, p.size / 2f), new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (p.opacity)).getRGB());
        }

        if (renderedHealth < entity.getHealth() + 0.1f) {
        } else {
            float highlightAlpha = Math.min(1.0f, (1.0f - displayedFill)) * fadeAlpha * 0.9f;
            if (highlightAlpha > 0.02f) {
                int ha = (int) (highlightAlpha * 120f);
                RenderUtil.drawRoundedRect(x + 5.0f, y + 4.0f, 24.0f, 24.0f, 12.0f, new Color(255, 255, 255, ha).getRGB());
            }
        }
    }

    private void renderTenacityHUD(EntityPlayer entity) {
        if (entity == null) return;

        float x = (float) posX.getValue();
        float y = (float) posY.getValue();

        float width = Math.max(155f, mc.fontRendererObj.getStringWidth(entity.getName()) + 75f);
        float height = 50f;

        float leftShift = 4.0f;
        x -= leftShift;

        this.width = (int) width;
        this.height = (int) height;

        float alpha = fadeAlpha;
        if (alpha <= 0f) return;

        renderShadow(x, y, width, height, 6.0f);

        long now = System.currentTimeMillis();
        long loop = Math.max(1, tenacitySwirlSpeed.getValue());
        long elapsed = (now - tenacityStartTime) % loop;
        float progress = (float) elapsed / (float) loop;

        Color baseLeft = new Color(255, 154, 0, (int) (alpha * 255f));
        Color baseRight = new Color(255, 64, 180, (int) (alpha * 255f));

        float t1 = (float) Math.sin(progress * MathHelper.PI * 2.0f) * 0.5f + 0.5f;
        float t2 = (float) Math.cos(progress * MathHelper.PI * 2.0f) * 0.5f + 0.5f;

        Color animLeft = lerpColor(baseLeft, baseRight, t1);
        Color animRight = lerpColor(baseRight, baseLeft, t2);

        RenderUtil.drawRoundedGradient(x, y, width, height, 6.0f, animLeft, animRight);

        if (tenacityUseGradient.isEnabled()) {
            final int bands = 3;
            float bandWidth = width / 3f;
            float bandAlphaFactor = (float) tenacitySwirlStrength.getValue() / 255f * alpha;

            for (int i = 0; i < bands; i++) {
                float bandProgress = (progress + (float) i / (float) bands) % 1.0f;
                float centerX = x + (bandProgress * (width + bandWidth)) - bandWidth * 0.5f;
                float bandW = bandWidth;
                float mix = (float) ((Math.sin((bandProgress + i * 0.33f) * MathHelper.PI * 2.0f) * 0.5f + 0.5f));
                Color bandColor = lerpColor(animLeft, animRight, mix);
                int bandA = Math.max(0, Math.min(255, (int) (bandAlphaFactor * 140f)));
                Color bandCol = new Color(bandColor.getRed(), bandColor.getGreen(), bandColor.getBlue(), bandA);

                RenderUtil.drawRoundedRect(centerX, y - height * 0.05f, bandW, height * 1.1f, height * 0.45f, bandCol.getRGB());
            }
        }

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        float size = 38f;
        float headX = x + 10f;
        float headY = y + (height / 2f) - (size / 2f);
        RenderUtil.drawRoundedRect(headX, headY, size, size, size / 2f, new Color(0, 0, 0, (int) (alpha * 153f)).getRGB());
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        DrawUtil.drawHead(((AbstractClientPlayer) entity).getLocationSkin(), (int) headX, (int) headY, (int) size, (int) size);
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        int textColor = new Color(255, 255, 255, (int) (alpha * 255f)).getRGB();

        float contentX = headX + size + 6.0f;
        float nameX = contentX;
        float nameY = y + 10f;
        FontUtil.drawStringWithShadow(font.getMode(), entity.getName(), (int) nameX, (int) nameY, textColor);

        float healthPercentage = (entity.getHealth() + entity.getAbsorptionAmount()) / (entity.getMaxHealth() + entity.getAbsorptionAmount());
        float healthBarWidth = width - (size + 36f);
        float newHealthWidth = healthBarWidth * Math.max(0f, Math.min(1f, healthPercentage));

        float currentWidth = displayedFill * healthBarWidth;
        currentWidth += (newHealthWidth - currentWidth) * 0.18f;
        displayedFill = Math.max(0f, Math.min(1f, currentWidth / Math.max(1f, healthBarWidth)));

        float barX = contentX;
        float barY = y + 25f;
        RenderUtil.drawRoundedRect(barX, barY, healthBarWidth, 4f, 2f, new Color(0, 0, 0, (int) (0.3f * alpha * 255f)).getRGB());

        RenderUtil.drawRoundedGradient(barX, barY, Math.max(1f, displayedFill * healthBarWidth), 4f, 2f, animLeft, animRight);

        int glowA = Math.max(0, Math.min(255, (int) (alpha * 100f)));
        RenderUtil.drawRoundedRect(barX, barY - 0.9f, Math.max(1f, displayedFill * healthBarWidth), 2f, 1f, new Color(255, 255, 255, glowA).getRGB());

        String healthText = Math.round(healthPercentage * 100f) + "%";
        FontUtil.drawStringWithShadow(font.getMode(), healthText + " - " + Math.round(mc.thePlayer.getDistanceToEntity(entity)) + "m",
                (int) (contentX), (int) (y + 35f), textColor);
    }

    private void renderAdjustHUD(EntityPlayer entity) {
        if (entity == null) return;

        if (target == null && !adjustFadingOut) {
            startAdjustFadeOut();
        } else if (target != null) {
            adjustFadingOut = false;
            adjustFadeOutTicks = 0;
        }

        if (adjustFollowPlayer.isEnabled()) {
            updateAdjustFollowPlayerPosition(entity);
        }

        float x = (float) posX.getValue();
        float y = (float) posY.getValue();

        String targetDisplayName = entity.getDisplayName().getFormattedText();
        int nameWidth = (int) FontUtil.getStringWidth(font.getMode(), targetDisplayName);

        float hudWidth = Math.max(145, nameWidth + 60);
        float hudHeight = 42;

        boolean showAvatar = adjustShowAvatar.isEnabled();
        float avatarSizeValue = 32f;
        float totalHudWidth = showAvatar ? hudWidth + avatarSizeValue + 4 : hudWidth;

        totalHudWidth = Math.min(totalHudWidth, 240);

        this.width = (int) totalHudWidth;
        this.height = (int) hudHeight;

        renderShadow(x, y, totalHudWidth, hudHeight, 3f);

        if (adjustFadingOut) {
            adjustFadeOutTicks++;

            adjustBgAlphaValue = (int) adjustLerp(adjustOriginalBackgroundAlpha, 0, (float) adjustFadeOutTicks / adjustMaxFadeOutTicks);
            adjustGhostAlphaValue = (int) adjustLerp(adjustOriginalGhostBarAlpha, 0, (float) adjustFadeOutTicks / adjustMaxFadeOutTicks);
            adjustDamageAlphaValue = (int) adjustLerp(adjustOriginalDamageAlpha, 0, (float) adjustFadeOutTicks / adjustMaxFadeOutTicks);

            if (adjustFadeOutTicks >= adjustMaxFadeOutTicks) {
                adjustFadingOut = false;
            }
        } else {
            adjustBgAlphaValue = adjustBackgroundAlpha.getValue();
            adjustGhostAlphaValue = adjustGhostBarAlpha.getValue();
            adjustDamageAlphaValue = adjustDamageAlphaSetting.getValue();
        }

        float alpha = fadeAlpha;
        int finalBgAlpha = (int) (adjustBgAlphaValue * alpha);
        int finalDamageAlpha = (int) (adjustDamageAlphaValue * alpha);

        Color backgroundColor = new Color(20, 20, 25, finalBgAlpha);
        RenderUtil.drawRoundedRect(x, y, totalHudWidth, hudHeight, 3f, backgroundColor.getRGB());

        float targetHealth = entity.getHealth();
        float previousHealth = adjustLastTargetHealth;
        adjustLastTargetHealth = targetHealth;

        float contentX = x;

        if (showAvatar) {
            float avatarX = x + 2;
            float avatarY = y + 2;

            RenderUtil.drawRoundedRect(avatarX, avatarY, avatarSizeValue, avatarSizeValue, 2f, new Color(40, 40, 45, finalBgAlpha).getRGB());

            if (entity instanceof AbstractClientPlayer) {
                GL11.glEnable(GL11.GL_STENCIL_TEST);
                GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

                RenderUtil.drawRoundedRect(avatarX, avatarY, avatarSizeValue, avatarSizeValue, 2f, 0xFFFFFFFF);
                GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

                DrawUtil.drawHead(((AbstractClientPlayer) entity).getLocationSkin(), (int) avatarX, (int) avatarY, (int) avatarSizeValue, (int) avatarSizeValue);
                GL11.glDisable(GL11.GL_STENCIL_TEST);
            }

            if (adjustDamageTicks > 0) {
                adjustDamageTicks--;
                int damageAlphaValue = (int) (finalDamageAlpha * ((float) adjustDamageTicks / adjustDamageDuration.getValue()));
                RenderUtil.drawRoundedRect(avatarX, avatarY, avatarSizeValue, avatarSizeValue, 2f, new Color(255, 0, 0, damageAlphaValue).getRGB());
            }

            contentX = avatarX + avatarSizeValue + 4;
        }

        int textColor = new Color(255, 255, 255, (int) (255 * alpha)).getRGB();
        FontUtil.drawStringWithShadow(font.getMode(), targetDisplayName, (int) contentX + 2, (int) (y + 4), textColor);

        float itemX = contentX + 2;
        float itemY = y + 15;
        float itemSize = 16;
        float itemSpacing = 18;

        ItemStack mainHandItem = entity.getHeldItem();
        if (mainHandItem != null) {
            renderAdjustItemIcon(mainHandItem, itemX, itemY, itemSize, alpha);
            itemX += itemSpacing;
        }

        for (int i = 0; i < 4; i++) {
            ItemStack armor = entity.getCurrentArmor(i);
            if (armor != null) {
                renderAdjustItemIcon(armor, itemX, itemY, itemSize, alpha);
                itemX += itemSpacing;
            }
        }

        EntityPlayer player = mc.thePlayer;
        if (player != null) {
            float playerHealth = player.getHealth();
            float healthDifference = playerHealth - targetHealth;
            String healthDiffText = String.format("%+.1f", healthDifference);

            float healthDiffX = (float) (x + totalHudWidth - FontUtil.getStringWidth(font.getMode(), healthDiffText) - 4);
            float healthDiffY = y + 4;

            int diffColor = healthDifference >= 0 ? new Color(100, 255, 100, (int) (255 * alpha)).getRGB() : new Color(255, 100, 100, (int) (255 * alpha)).getRGB();
            FontUtil.drawStringWithShadow(font.getMode(), healthDiffText, (int) healthDiffX, (int) healthDiffY, diffColor);
        }
    }

    private void renderAdjustItemIcon(ItemStack item, float x, float y, float size, float alpha) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);

        RenderUtil.drawRoundedRect(0, 0, size, size, 2f, new Color(30, 30, 35, (int) (200 * alpha)).getRGB());

        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            mc.getRenderItem().renderItemIntoGUI(item, 0, 0);

        } catch (Exception e) {
            RenderUtil.drawRoundedRect(3, 3, size - 6, size - 6, 1f, new Color(200, 200, 200, (int) (200 * alpha)).getRGB());
        }

        GL11.glPopMatrix();
    }

    private void startAdjustFadeOut() {
        adjustFadingOut = true;
        adjustFadeOutTicks = 0;

        adjustOriginalBackgroundAlpha = adjustBackgroundAlpha.getValue();
        adjustOriginalGhostBarAlpha = adjustGhostBarAlpha.getValue();
        adjustOriginalDamageAlpha = adjustDamageAlphaSetting.getValue();
    }

    private void updateAdjustFollowPlayerPosition(EntityPlayer entity) {
        Vec3 position = new Vec3(entity.posX, entity.posY, entity.posZ);
        Vec3 lastPosition = new Vec3(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ);

        double partialTicks = mc.timer.renderPartialTicks;
        double interpolatedX = adjustInterpolate(position.xCoord, lastPosition.xCoord, partialTicks);
        double interpolatedY = adjustInterpolate(position.yCoord, lastPosition.yCoord, partialTicks);
        double interpolatedZ = adjustInterpolate(position.zCoord, lastPosition.zCoord, partialTicks);

        ScaledResolution sr = new ScaledResolution(mc);
        Vec3 screenPos = worldToScreen(interpolatedX, interpolatedY + entity.height, interpolatedZ, sr.getScaledWidth(), sr.getScaledHeight(), (float) partialTicks);

        if (screenPos != null) {
            this.posX.setValue((int) (screenPos.xCoord - 50));
            this.posY.setValue((int) (screenPos.yCoord - 25));
        }
    }

    private Vec3 worldToScreen(double x, double y, double z, int displayWidth, int displayHeight, float partialTicks) {
        double diffX = x - mc.getRenderManager().viewerPosX;
        double diffY = y - mc.getRenderManager().viewerPosY;
        double diffZ = z - mc.getRenderManager().viewerPosZ;

        double distance = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
        if (distance > 64) return null;

        float scale = (float) (displayHeight / 2.0 / Math.tan(Math.toRadians(mc.gameSettings.fovSetting) / 2.0));

        double screenX = displayWidth / 2.0 + (diffX / diffZ) * scale;
        double screenY = displayHeight / 2.0 + (diffY / diffZ) * scale;

        return new Vec3(screenX, screenY, 0);
    }

    private int getColorModeIndex(String mode) {
        String[] modes = {"Cherry", "Cotton Candy", "Flare", "Flower", "Gold", "Grayscale", "Royal", "Sky", "Vine",
                "Red", "Blue", "Idk", "Idk2", "Opal", "Candy Cane", "Idklol", "What", "Sunset"};
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(mode)) {
                return i;
            }
        }
        return 0;
    }

    private Color darker(Color c) {
        float factor = 0.7f;
        int r = Math.max(0, Math.min(255, (int) (c.getRed() * factor)));
        int g = Math.max(0, Math.min(255, (int) (c.getGreen() * factor)));
        int b = Math.max(0, Math.min(255, (int) (c.getBlue() * factor)));
        return new Color(r, g, b, c.getAlpha());
    }

    private Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int aa = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, bl)), Math.max(0, Math.min(255, aa)));
    }

    private double adjustInterpolate(double current, double old, double partialTicks) {
        return old + (current - old) * partialTicks;
    }

    private float adjustLerp(float start, float end, float speed) {
        return start + (end - start) * speed;
    }

    private static float easeOutCubic(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float p = t - 1.0f;
        return p * p * p + 1.0f;
    }

    private float targetFill(float currentHealth, EntityPlayer entity) {
        float totMax = Math.max(1.0f, entity.getMaxHealth());
        return Math.max(0f, Math.min(1f, currentHealth / totMax));
    }

    public static class Particle {
        public float x, y, adjustedX, adjustedY, deltaX, deltaY, size, opacity;
        public Color color;

        public Particle() {
            x = 0f;
            y = 0f;
            adjustedX = 0f;
            adjustedY = 0f;
            deltaX = 0f;
            deltaY = 0f;
            size = 2f;
            opacity = 254f;
            color = Color.WHITE;
        }

        public void render2D() {
            RenderUtil.drawRoundedRect(x + adjustedX, y + adjustedY, size, size, (size / 2f) - 0.5f,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.max(1f, opacity)).getRGB());
        }

        public void updatePosition() {
            for (int i = 0; i < 2; i++) {
                adjustedX += deltaX;
                adjustedY += deltaY;
                deltaY *= 0.97f;
                deltaX *= 0.97f;
                opacity -= 1.5f;
                if (opacity < 1f) opacity = 0f;
            }
        }

        public void init(float ix, float iy, float iDeltaX, float iDeltaY, float iSize, Color icolor) {
            this.x = ix;
            this.y = iy;
            this.deltaX = iDeltaX;
            this.deltaY = iDeltaY;
            this.size = iSize;
            this.opacity = 254f;
            this.color = icolor;
            this.adjustedX = 0f;
            this.adjustedY = 0f;
        }
    }
}