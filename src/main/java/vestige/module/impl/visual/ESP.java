package vestige.module.impl.visual;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector4f;
import vestige.event.Listener;
import vestige.event.impl.RenderEvent;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.IntegerSetting;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ESP extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting chests = new BooleanSetting("Chests", false);
    private final BooleanSetting filled = new BooleanSetting("Filled", false);
    
    private final IntegerSetting playerColorR = new IntegerSetting("Player Red", 0, 0, 255, 1);
    private final IntegerSetting playerColorG = new IntegerSetting("Player Green", 255, 0, 255, 1);
    private final IntegerSetting playerColorB = new IntegerSetting("Player Blue", 255, 0, 255, 1);
    
    private final IntegerSetting chestColorR = new IntegerSetting("Chest Red", 255, 0, 255, 1);
    private final IntegerSetting chestColorG = new IntegerSetting("Chest Green", 140, 0, 255, 1);
    private final IntegerSetting chestColorB = new IntegerSetting("Chest Blue", 0, 0, 255, 1);

    private final Map<Object, Vector4f> entityPosition = new HashMap<>();

    public ESP() {
        super("ESP", Category.VISUAL);
        addSettings(players, chests, filled, playerColorR, playerColorG, playerColorB, 
                   chestColorR, chestColorG, chestColorB);
    }

    @Listener
    public void onRender(RenderEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        entityPosition.clear();
        
        if (players.isEnabled()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (shouldRenderEntity(entity) && isInView(entity)) {
                    Vector4f pos = getEntityPositionsOn2D(entity);
                    if (pos != null) {
                        entityPosition.put(entity, pos);
                    }
                }
            }
        }
        
        if (chests.isEnabled()) {
            for (TileEntity tile : mc.theWorld.loadedTileEntityList) {
                if (shouldRenderChest(tile)) {
                    Vector4f pos = getChestPositionsOn2D(tile);
                    if (pos != null) {
                        entityPosition.put(tile, pos);
                    }
                }
            }
        }
        
        renderESP();
    }

    private void renderESP() {
        for (Object obj : entityPosition.keySet()) {
            Vector4f pos = entityPosition.get(obj);
            float x = pos.getX();
            float y = pos.getY();
            float right = pos.getZ();
            float bottom = pos.getW();
            
            Color color;
            if (obj instanceof EntityPlayer) {
                color = new Color(playerColorR.getValue(), playerColorG.getValue(), playerColorB.getValue());
            } else {
                color = new Color(chestColorR.getValue(), chestColorG.getValue(), chestColorB.getValue());
            }
            
            renderBox(x, y, right, bottom, color);
        }
    }

    private void renderBox(float x, float y, float right, float bottom, Color color) {
        float lineWidth = 2.0f;
        
        if (filled.isEnabled()) {
            Gui.drawRect((int)x, (int)y, (int)right, (int)bottom, 
                       new Color(color.getRed(), color.getGreen(), color.getBlue(), 50).getRGB());
        }
        
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(lineWidth);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1.0f);
        
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(right, y);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(x, bottom);
        GL11.glEnd();
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private Vector4f getEntityPositionsOn2D(Entity entity) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().viewerPosZ;
        
        double width = entity.width / 2.0;
        double height = entity.height + 0.1;
        
        double[][] positions = {
            {x - width, y, z - width}, {x + width, y, z - width},
            {x - width, y, z + width}, {x + width, y, z + width},
            {x - width, y + height, z - width}, {x + width, y + height, z - width},
            {x - width, y + height, z + width}, {x + width, y + height, z + width}
        };
        
        return calculateScreenBounds(positions);
    }

    private Vector4f getChestPositionsOn2D(TileEntity chest) {
        double x = chest.getPos().getX() - mc.getRenderManager().viewerPosX;
        double y = chest.getPos().getY() - mc.getRenderManager().viewerPosY;
        double z = chest.getPos().getZ() - mc.getRenderManager().viewerPosZ;
        
        double[][] positions = {
            {x, y, z}, {x + 1, y, z},
            {x, y, z + 1}, {x + 1, y, z + 1},
            {x, y + 1, z}, {x + 1, y + 1, z},
            {x, y + 1, z + 1}, {x + 1, y + 1, z + 1}
        };
        
        return calculateScreenBounds(positions);
    }

    private Vector4f calculateScreenBounds(double[][] positions) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -1f;
        float maxY = -1f;
        
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

    private boolean shouldRenderEntity(Entity entity) {
        if (entity.isDead || entity == mc.thePlayer) return false;
        return entity instanceof EntityPlayer;
    }

    private boolean shouldRenderChest(TileEntity tile) {
        return tile instanceof TileEntityChest || tile instanceof TileEntityEnderChest;
    }
}
