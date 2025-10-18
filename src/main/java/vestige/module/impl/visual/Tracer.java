package vestige.module.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import vestige.event.Listener;
import vestige.event.impl.Render3DEvent;
import vestige.module.Module;
import vestige.module.Category;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.IntegerSetting;
import vestige.setting.impl.ModeSetting;

import java.awt.Color;

public class Tracer extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Players", "Players", "Animals", "Items");
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", false);
    private final IntegerSetting red = new IntegerSetting("Red", 255, 0, 255, 1);
    private final IntegerSetting green = new IntegerSetting("Green", 255, 0, 255, 1);
    private final IntegerSetting blue = new IntegerSetting("Blue", 255, 0, 255, 1);

    public Tracer() {
        super("Tracer", Category.VISUAL);
        addSettings(mode, rainbow, red, green, blue);
    }

    @Listener
    public void onRender3D(Render3DEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) continue;

            if (mode.is("Players") && entity instanceof EntityPlayer ||
                    mode.is("Animals") && entity instanceof EntityAnimal ||
                    mode.is("Items") && entity instanceof EntityItem) {

                double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.getPartialTicks() - mc.getRenderManager().viewerPosX;
                double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.getPartialTicks() - mc.getRenderManager().viewerPosY;
                double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.getPartialTicks() - mc.getRenderManager().viewerPosZ;

                Color color = rainbow.isEnabled() ? Color.getHSBColor((System.currentTimeMillis() % 10000) / 10000f, 1f, 1f) : new Color(red.getValue(), green.getValue(), blue.getValue());

                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(1.5f);
                GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f);

                GL11.glBegin(GL11.GL_LINES);
                GL11.glVertex3d(0, mc.thePlayer.getEyeHeight(), 0);
                GL11.glVertex3d(x, y + entity.height / 2.0, z);
                GL11.glEnd();

                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            }
        }
    }
}
