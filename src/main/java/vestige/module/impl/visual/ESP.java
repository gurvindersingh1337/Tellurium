package vestige.module.impl.visual;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StringUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector4f;
import vestige.Vestige;
import vestige.event.Listener;
import vestige.event.impl.RenderEvent;
import vestige.font.VestigeFontRenderer;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.DoubleSetting;
import vestige.setting.impl.IntegerSetting;
import vestige.setting.impl.ModeSetting;
import vestige.util.render.RenderUtil;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class ESP extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    
    private final BooleanSetting mcfont = new BooleanSetting("Minecraft Font", false);
    private final BooleanSetting boxEsp = new BooleanSetting("Box", true);
    private final ModeSetting boxColorMode = new ModeSetting("Box Mode", "Custom", "Custom", "Client");
    private final IntegerSetting boxColorR = new IntegerSetting("Box Red", 0, 0, 255, 1);
    private final IntegerSetting boxColorG = new IntegerSetting("Box Green", 255, 0, 255, 1);
    private final IntegerSetting boxColorB = new IntegerSetting("Box Blue", 255, 0, 255, 1);
    
    private final BooleanSetting itemHeld = new BooleanSetting("Item Held", true);
    private final BooleanSetting equipment = new BooleanSetting("Equipment", true);
    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
    private final ModeSetting healthBarMode = new ModeSetting("Health Mode", "Color", "Health", "Color");
    private final BooleanSetting healthBarText = new BooleanSetting("Health Text", true);
    
    private final BooleanSetting nametags = new BooleanSetting("Nametags", true);
    private final DoubleSetting scale = new DoubleSetting("Tag Scale", 0.75, 0.35, 1.0, 0.05);
    private final BooleanSetting healthText = new BooleanSetting("Health Tag", true);
    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final BooleanSetting rounded = new BooleanSetting("Rounded", true);

    private final Map<Entity, Vector4f> entityPosition = new HashMap<>();
    private final NumberFormat df = new DecimalFormat("0.#");
    private final Color backgroundColor = new Color(10, 10, 10, 130);
    
    private Color firstColor = Color.CYAN;
    private Color secondColor = Color.CYAN;
    private Color thirdColor = Color.CYAN;
    private Color fourthColor = Color.CYAN;
    
    private VestigeFontRenderer font;

    public ESP() {
        super("ESP", Category.VISUAL);
        addSettings(players, animals, mobs, mcfont, boxEsp, boxColorMode, boxColorR, boxColorG, boxColorB,
                itemHeld, equipment, healthBar, healthBarMode, healthBarText,
                nametags, scale, healthText, background, rounded);
    }

    @Override
    public void onClientStarted() {
        font = Vestige.instance.getFontManager().getComfortaa();
    }

    @Listener
    public void onRender(RenderEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        entityPosition.clear();
        
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (shouldRender(entity) && isInView(entity)) {
                Vector4f pos = getEntityPositionsOn2D(entity);
                if (pos != null) {
                    entityPosition.put(entity, pos);
                }
            }
        }
        
        updateColors();
        renderESP();
    }

    private void updateColors() {
        if (boxColorMode.is("Custom")) {
            Color baseColor = new Color(boxColorR.getValue(), boxColorG.getValue(), boxColorB.getValue());
            firstColor = baseColor;
            secondColor = baseColor;
            thirdColor = baseColor;
            fourthColor = baseColor;
        } else {
            long time = System.currentTimeMillis();
            firstColor = getColorFromTime(time, 0);
            secondColor = getColorFromTime(time, 90);
            thirdColor = getColorFromTime(time, 180);
            fourthColor = getColorFromTime(time, 270);
        }
    }

    private Color getColorFromTime(long time, int offset) {
        float hue = ((time + offset * 10) % 2000) / 2000.0f;
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    private void renderESP() {
        for (Entity entity : entityPosition.keySet()) {
            Vector4f pos = entityPosition.get(entity);
            float x = pos.getX();
            float y = pos.getY();
            float right = pos.getZ();
            float bottom = pos.getW();

            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                VestigeFontRenderer fontRenderer = mcfont.isEnabled() ? mc.fontRendererObj : font;
                
                if (nametags.isEnabled()) {
                    renderNametag(living, fontRenderer, x, y, right, bottom);
                }
                
                if (itemHeld.isEnabled() && living.getHeldItem() != null) {
                    renderHeldItem(living, fontRenderer, x, right, bottom);
                }
                
                if (equipment.isEnabled()) {
                    renderEquipment(living, x, y, right, bottom);
                }
                
                if (healthBar.isEnabled()) {
                    renderHealthBar(living, x, y, bottom);
                }
            }
            
            if (boxEsp.isEnabled()) {
                renderBox(x, y, right, bottom);
            }
        }
    }

    private void renderNametag(EntityLivingBase entity, VestigeFontRenderer fontRenderer, float x, float y, float right, float bottom) {
        float healthValue = entity.getHealth() / entity.getMaxHealth();
        Color healthColor = healthValue > 0.75f ? new Color(66, 246, 123) : 
                           healthValue > 0.5f ? new Color(228, 255, 105) : 
                           healthValue > 0.35f ? new Color(236, 100, 64) : 
                           new Color(255, 65, 68);
        
        String name = StringUtils.stripControlCodes(entity.getDisplayName().getUnformattedText());
        StringBuilder text = new StringBuilder("§f" + name);
        
        if (healthText.isEnabled()) {
            text.append(String.format(" §7[§r%s HP§7]", df.format(entity.getHealth())));
        }
        
        float fontScale = (float)scale.getValue();
        float middle = x + ((right - x) / 2);
        float textWidth = fontRenderer.getStringWidth(text.toString());
        middle -= (textWidth * fontScale) / 2f;
        float fontHeight = fontRenderer.getHeight() * fontScale;
        
        GL11.glPushMatrix();
        GL11.glTranslated(middle, y - (fontHeight + 2), 0);
        GL11.glScaled(fontScale, fontScale, 1);
        GL11.glTranslated(-middle, -(y - (fontHeight + 2)), 0);
        
        if (background.isEnabled()) {
            if (rounded.isEnabled()) {
                RenderUtil.drawRoundedRect(middle - 3, y - (fontHeight + 7), textWidth + 6,
                        (fontHeight / fontScale) + 4, 4, backgroundColor.getRGB());
            } else {
                Gui.drawRect((int)(middle - 3), (int)(y - (fontHeight + 7)), 
                           (int)(middle + textWidth + 3), (int)(y - (fontHeight + 7) + (fontHeight / fontScale) + 4), 
                           backgroundColor.getRGB());
            }
        }
        
        if (mcfont.isEnabled()) {
            mc.fontRendererObj.drawString(StringUtils.stripControlCodes(text.toString()), 
                                         middle + 0.5f, y - (fontHeight + 4) + 0.5f, Color.BLACK.getRGB());
            mc.fontRendererObj.drawString(text.toString(), middle, y - (fontHeight + 4), healthColor.getRGB());
        } else {
            fontRenderer.drawStringWithShadow(text.toString(), middle, y - (fontHeight + 5), healthColor.getRGB());
        }
        
        GL11.glPopMatrix();
    }

    private void renderHeldItem(EntityLivingBase entity, VestigeFontRenderer fontRenderer, float x, float right, float bottom) {
        float fontScale = 0.5f;
        float middle = x + ((right - x) / 2);
        String text = entity.getHeldItem().getDisplayName();
        float textWidth = fontRenderer.getStringWidth(text);
        middle -= (textWidth * fontScale) / 2f;
        
        GL11.glPushMatrix();
        GL11.glTranslated(middle, bottom + 4, 0);
        GL11.glScaled(fontScale, fontScale, 1);
        GL11.glTranslated(-middle, -(bottom + 4), 0);
        
        Gui.drawRect((int)(middle - 3), (int)(bottom + 1), 
                   (int)(middle + textWidth + 3), (int)(bottom + 1 + fontRenderer.getHeight() + 5), 
                   backgroundColor.getRGB());
        
        if (mcfont.isEnabled()) {
            mc.fontRendererObj.drawStringWithShadow(text, middle, bottom + 4, -1);
        } else {
            fontRenderer.drawStringWithShadow(text, middle, bottom + 4, -1);
        }
        
        GL11.glPopMatrix();
    }

    private void renderEquipment(EntityLivingBase entity, float x, float y, float right, float bottom) {
        float scale = 0.4f;
        float equipmentX = right + 5;
        float equipmentY = y - 1;
        
        GL11.glPushMatrix();
        GL11.glTranslated(equipmentX, equipmentY, 0);
        GL11.glScaled(scale, scale, 1);
        GL11.glTranslated(-equipmentX, -y, 0);
        
        RenderHelper.enableGUIStandardItemLighting();
        float separation = 0f;
        float length = (bottom - y) - 2;
        
        for (int i = 3; i >= 0; i--) {
            if (entity.getCurrentArmor(i) != null) {
                mc.getRenderItem().renderItemAndEffectIntoGUI(entity.getCurrentArmor(i), 
                                                             (int)equipmentX, (int)(equipmentY + separation));
            }
            separation += (length / 3) / scale;
        }
        
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
    }

    private void renderHealthBar(EntityLivingBase entity, float x, float y, float bottom) {
        float healthValue = entity.getHealth() / entity.getMaxHealth();
        Color healthColor = healthValue > 0.75f ? new Color(66, 246, 123) : 
                           healthValue > 0.5f ? new Color(228, 255, 105) : 
                           healthValue > 0.35f ? new Color(236, 100, 64) : 
                           new Color(255, 65, 68);
        
        float height = (bottom - y) + 1;
        Gui.drawRect((int)(x - 3.5f), (int)(y - 0.5f), (int)(x - 1.5f), (int)(bottom + 0.5f), 
                   new Color(0, 0, 0, 180).getRGB());
        
        if (healthBarMode.is("Color")) {
            drawGradient(x - 3, y, 1, height, 0.3f, firstColor, fourthColor);
            drawGradient(x - 3, y + (height - (height * healthValue)), 1, height * healthValue, 1, firstColor, fourthColor);
        } else {
            Gui.drawRect((int)(x - 3), (int)y, (int)(x - 2), (int)(y + height), 
                       new Color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), 76).getRGB());
            Gui.drawRect((int)(x - 3), (int)(y + (height - (height * healthValue))), 
                       (int)(x - 2), (int)(y + height), healthColor.getRGB());
        }
        
        if (healthBarText.isEnabled()) {
            VestigeFontRenderer fontRenderer = mcfont.isEnabled() ? mc.fontRendererObj : font;
            healthValue *= 100;
            String health;
            if (healthValue >= 99.5f) {
                health = "100";
            } else {
                health = String.format("%.0f", healthValue);
            }
            String text = health + "%";
            float fontScale = 0.5f;
            float textX = x - ((fontRenderer.getStringWidth(text) / 2f) + 2);
            float fontHeight = fontRenderer.getHeight() * fontScale;
            float newHeight = height - fontHeight;
            float textY = y + (newHeight - (newHeight * (healthValue / 100)));
            
            GL11.glPushMatrix();
            GL11.glTranslated(textX - 5, textY, 1);
            GL11.glScaled(fontScale, fontScale, 1);
            GL11.glTranslated(-(textX - 5), -textY, 1);
            
            if (mcfont.isEnabled()) {
                mc.fontRendererObj.drawStringWithShadow(text, textX, textY, -1);
            } else {
                fontRenderer.drawStringWithShadow(text, textX, textY, -1);
            }
            
            GL11.glPopMatrix();
        }
    }

    private void renderBox(float x, float y, float right, float bottom) {
        float outlineThickness = 0.5f;
        
        drawGradientLR(x, y, right - x, 1, 1, firstColor, secondColor);
        drawGradient(x, y, 1, bottom - y, 1, firstColor, fourthColor);
        drawGradientLR(x, bottom, right - x, 1, 1, fourthColor, thirdColor);
        drawGradient(right, y, 1, (bottom - y) + 1, 1, secondColor, thirdColor);
        
        Gui.drawRect((int)(x - 0.5f), (int)(y - outlineThickness), (int)(right + 1.5f), (int)y, Color.BLACK.getRGB());
        Gui.drawRect((int)(x - outlineThickness), (int)y, (int)x, (int)(bottom + 1), Color.BLACK.getRGB());
        Gui.drawRect((int)(x - 0.5f), (int)(bottom + 1), (int)(right + 1.5f), (int)(bottom + 1 + outlineThickness), Color.BLACK.getRGB());
        Gui.drawRect((int)(right + 1), (int)y, (int)(right + 1 + outlineThickness), (int)(bottom + 1), Color.BLACK.getRGB());
        
        Gui.drawRect((int)(x + 1), (int)(y + 1), (int)right, (int)(y + 1 + outlineThickness), Color.BLACK.getRGB());
        Gui.drawRect((int)(x + 1), (int)(y + 1), (int)(x + 1 + outlineThickness), (int)bottom, Color.BLACK.getRGB());
        Gui.drawRect((int)(x + 1), (int)(bottom - outlineThickness), (int)right, (int)bottom, Color.BLACK.getRGB());
        Gui.drawRect((int)(right - outlineThickness), (int)(y + 1), (int)right, (int)bottom, Color.BLACK.getRGB());
    }

    private void drawGradient(float x, float y, float width, float height, float alpha, Color top, Color bottom) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(top.getRed() / 255f, top.getGreen() / 255f, top.getBlue() / 255f, alpha);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glColor4f(bottom.getRed() / 255f, bottom.getGreen() / 255f, bottom.getBlue() / 255f, alpha);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawGradientLR(float x, float y, float width, float height, float alpha, Color left, Color right) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(left.getRed() / 255f, left.getGreen() / 255f, left.getBlue() / 255f, alpha);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + height);
        GL11.glColor4f(right.getRed() / 255f, right.getGreen() / 255f, right.getBlue() / 255f, alpha);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private Vector4f getEntityPositionsOn2D(Entity entity) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().viewerPosZ;
        
        double width = entity.width / 2;
        double height = entity.height + 0.1;
        
        double[][] positions = {
            {x - width, y, z - width}, {x + width, y, z - width},
            {x - width, y, z + width}, {x + width, y, z + width},
            {x - width, y + height, z - width}, {x + width, y + height, z - width},
            {x - width, y + height, z + width}, {x + width, y + height, z + width}
        };
        
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -1, maxY = -1;
        
        for (double[] pos : positions) {
            float[] screenPos = worldToScreen(pos[0], pos[1], pos[2]);
            if (screenPos == null) return null;
            minX = Math.min(screenPos[0], minX);
            minY = Math.min(screenPos[1], minY);
            maxX = Math.max(screenPos[0], maxX);
            maxY = Math.max(screenPos[1], maxY);
        }
        
        return new Vector4f(minX, minY, maxX, maxY);
    }

    private float[] worldToScreen(double x, double y, double z) {
        java.nio.IntBuffer viewport = org.lwjgl.BufferUtils.createIntBuffer(16);
        java.nio.FloatBuffer modelview = org.lwjgl.BufferUtils.createFloatBuffer(16);
        java.nio.FloatBuffer projection = org.lwjgl.BufferUtils.createFloatBuffer(16);
        java.nio.FloatBuffer coords = org.lwjgl.BufferUtils.createFloatBuffer(3);
        
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        
        boolean result = org.lwjgl.util.glu.GLU.gluProject((float)x, (float)y, (float)z, modelview, projection, viewport, coords);
        
        if (result) {
            return new float[]{coords.get(0), mc.displayHeight - coords.get(1)};
        }
        return null;
    }

    private boolean isInView(Entity entity) {
        float[] screenPos = worldToScreen(
            entity.posX - mc.getRenderManager().viewerPosX,
            entity.posY - mc.getRenderManager().viewerPosY,
            entity.posZ - mc.getRenderManager().viewerPosZ
        );
        return screenPos != null;
    }

    private boolean shouldRender(Entity entity) {
        if (entity.isDead || entity.isInvisible() || entity == mc.thePlayer) return false;
        
        if (players.isEnabled() && entity instanceof EntityPlayer) {
            return !entity.getDisplayName().getUnformattedText().contains("[NPC");
        }
        if (animals.isEnabled() && entity instanceof EntityAnimal) return true;
        if (mobs.isEnabled() && entity instanceof EntityMob) return true;
        
        return false;
    }
}
