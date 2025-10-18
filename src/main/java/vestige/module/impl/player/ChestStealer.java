package vestige.module.impl.player;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import vestige.Vestige;
import vestige.event.Listener;
import vestige.event.impl.RenderEvent;
import vestige.event.impl.UpdateEvent;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.IntegerSetting;
import vestige.util.render.ColorUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class ChestStealer extends Module {

    private final IntegerSetting delay = new IntegerSetting("Delay", 1, 0, 10, 1);
    private final BooleanSetting filter = new BooleanSetting("Filter", true);
    private final BooleanSetting autoClose = new BooleanSetting("Autoclose", true);
    private final BooleanSetting guiDetect = new BooleanSetting("Gui detect", true);
    private final BooleanSetting visual = new BooleanSetting("Visual", true);
    private final IntegerSetting colorRed = new IntegerSetting("Red", 100, 0, 255, 1);
    private final IntegerSetting colorGreen = new IntegerSetting("Green", 255, 0, 255, 1);
    private final IntegerSetting colorBlue = new IntegerSetting("Blue", 150, 0, 255, 1);
    private final IntegerSetting color2Red = new IntegerSetting("Red 2", 50, 0, 255, 1);
    private final IntegerSetting color2Green = new IntegerSetting("Green 2", 200, 0, 255, 1);
    private final IntegerSetting color2Blue = new IntegerSetting("Blue 2", 255, 0, 255, 1);

    private int counter;
    private InventoryManager invManager;
    private ArrayList<StealParticle> particles = new ArrayList<>();
    private int totalItems = 0;
    private int stolenItems = 0;
    private long stealStartTime = 0;
    private float animationProgress = 0;

    public ChestStealer() {
        super("Chest Stealer", Category.PLAYER);
        this.addSettings(delay, filter, autoClose, guiDetect, visual, colorRed, colorGreen, colorBlue, color2Red, color2Green, color2Blue);
    }

    @Override
    public void onClientStarted() {
        invManager = Vestige.instance.getModuleManager().getModule(InventoryManager.class);
    }

    @Listener
    public void onUpdate(UpdateEvent event) {
        if(invManager == null) return;

        if(mc.thePlayer.openContainer != null && mc.thePlayer.openContainer instanceof ContainerChest && (!isGUI() || !guiDetect.isEnabled())) {
            ContainerChest container = (ContainerChest) mc.thePlayer.openContainer;

            if(stealStartTime == 0) {
                stealStartTime = System.currentTimeMillis();
                totalItems = countItems(container);
                stolenItems = 0;
                animationProgress = 0;
            }

            for(int i = 0; i < container.getLowerChestInventory().getSizeInventory(); i++) {
                ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);
                if(stack != null && !isUseless(stack)) {
                    if(++counter > delay.getValue()) {
                        mc.playerController.windowClick(container.windowId, i, 1, 1, mc.thePlayer);
                        counter = 0;
                        stolenItems++;

                        if(visual.isEnabled()) {
                            try {
                                for(int j = 0; j < 15; j++) {
                                    particles.add(new StealParticle());
                                }
                            } catch(Exception e) {
                            }
                        }
                        return;
                    }
                }
            }

            if(autoClose.isEnabled() && isChestEmpty(container)) {
                mc.thePlayer.closeScreen();
                stealStartTime = 0;
            }
        } else {
            stealStartTime = 0;
        }
    }

    @Listener
    public void onRender(RenderEvent event) {
        if(!visual.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        try {
            ScaledResolution sr = new ScaledResolution(mc);
            int width = sr.getScaledWidth();
            int height = sr.getScaledHeight();

            Iterator<StealParticle> iterator = particles.iterator();
            while(iterator.hasNext()) {
                StealParticle particle = iterator.next();
                if(particle.age > 40) {
                    iterator.remove();
                    continue;
                }

                particle.update();

                float progress = particle.age / 40.0f;
                float alpha = (float) Math.sin(progress * Math.PI);
                float scale = 1.0f + (float) Math.sin(progress * Math.PI) * 0.5f;

                Color color1 = new Color(colorRed.getValue(), colorGreen.getValue(), colorBlue.getValue());
                Color color2 = new Color(color2Red.getValue(), color2Green.getValue(), color2Blue.getValue());
                Color blendedColor = ColorUtil.getGradient(color1, color2, progress);
                int finalColor = new Color(blendedColor.getRed(), blendedColor.getGreen(), blendedColor.getBlue(), (int)(alpha * 255)).getRGB();

                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

                drawCircle(particle.x, particle.y, scale * 3, finalColor);
                drawCircle(particle.x, particle.y, scale * 1.5f, new Color(255, 255, 255, (int)(alpha * 200)).getRGB());

                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            }

            if(mc.thePlayer.openContainer instanceof ContainerChest && stealStartTime > 0 && totalItems > 0) {
                float targetProgress = Math.min(1.0f, (float) stolenItems / totalItems);
                animationProgress += (targetProgress - animationProgress) * 0.1f;

                int barWidth = 220;
                int barHeight = 8;
                int barX = width / 2 - barWidth / 2;
                int barY = height / 2 + 90;

                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                drawRoundedRect(barX - 3, barY - 3, barX + barWidth + 3, barY + barHeight + 3, 4, new Color(0, 0, 0, 180).getRGB());
                drawRoundedRect(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 3, new Color(20, 20, 20, 220).getRGB());

                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();

                Color color1 = new Color(colorRed.getValue(), colorGreen.getValue(), colorBlue.getValue());
                Color color2 = new Color(color2Red.getValue(), color2Green.getValue(), color2Blue.getValue());

                int progressWidth = (int)(barWidth * animationProgress);

                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glShadeModel(GL11.GL_SMOOTH);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                for(int i = 0; i < progressWidth; i++) {
                    float ratio = progressWidth > 0 ? (float) i / barWidth : 0;
                    Color gradientColor = ColorUtil.getGradient(color1, color2, ratio);
                    drawRoundedRect(barX + i, barY, barX + i + 1, barY + barHeight, 0, gradientColor.getRGB());
                }

                float wave = (float) Math.sin((System.currentTimeMillis() % 2000) / 2000.0 * Math.PI * 2) * 0.3f + 0.7f;
                for(int i = 0; i < progressWidth; i++) {
                    float ratio = progressWidth > 0 ? (float) i / barWidth : 0;
                    Color gradientColor = ColorUtil.getGradient(color1, color2, ratio);
                    Color glowColor = new Color(gradientColor.getRed(), gradientColor.getGreen(), gradientColor.getBlue(), (int)(100 * wave));
                    Gui.drawRect(barX + i, barY - 2, barX + i + 1, barY, glowColor.getRGB());
                    Gui.drawRect(barX + i, barY + barHeight, barX + i + 1, barY + barHeight + 2, glowColor.getRGB());
                }

                GL11.glShadeModel(GL11.GL_FLAT);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();

                String text = "STEALING " + stolenItems + "/" + totalItems;
                float textScale = 0.8f;
                GL11.glPushMatrix();
                GL11.glScalef(textScale, textScale, textScale);
                int scaledX = (int)((width / 2 - mc.fontRendererObj.getStringWidth(text) * textScale / 2) / textScale);
                int scaledY = (int)((barY - 18) / textScale);

                Color textColor = ColorUtil.getGradient(color1, color2, (float)((System.currentTimeMillis() % 2000) / 2000.0));
                mc.fontRendererObj.drawStringWithShadow(text, scaledX, scaledY, textColor.getRGB());
                GL11.glPopMatrix();
            }
        } catch(Exception e) {
        }
    }

    private void drawCircle(float x, float y, float radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0f;
        float red = (color >> 16 & 0xFF) / 255.0f;
        float green = (color >> 8 & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);
        for(int i = 0; i <= 20; i++) {
            double angle = 2 * Math.PI * i / 20;
            GL11.glVertex2f((float)(x + Math.cos(angle) * radius), (float)(y + Math.sin(angle) * radius));
        }
        GL11.glEnd();
    }

    private void drawRoundedRect(float x, float y, float x2, float y2, float radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0f;
        float red = (color >> 16 & 0xFF) / 255.0f;
        float green = (color >> 8 & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        GL11.glColor4f(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_POLYGON);
        for(int i = 0; i <= 90; i += 10) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f((float)(x2 - radius + Math.cos(angle) * radius), (float)(y + radius - Math.sin(angle) * radius));
        }
        for(int i = 90; i <= 180; i += 10) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f((float)(x + radius + Math.cos(angle) * radius), (float)(y + radius - Math.sin(angle) * radius));
        }
        for(int i = 180; i <= 270; i += 10) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f((float)(x + radius + Math.cos(angle) * radius), (float)(y2 - radius - Math.sin(angle) * radius));
        }
        for(int i = 270; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f((float)(x2 - radius + Math.cos(angle) * radius), (float)(y2 - radius - Math.sin(angle) * radius));
        }
        GL11.glEnd();
    }

    private int countItems(ContainerChest container) {
        int count = 0;
        try {
            for(int i = 0; i < container.getLowerChestInventory().getSizeInventory(); i++) {
                ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);
                if(stack != null && !isUseless(stack)) {
                    count++;
                }
            }
        } catch(Exception e) {
        }
        return count;
    }

    private boolean isChestEmpty(ContainerChest container) {
        for(int i = 0; i < container.getLowerChestInventory().getSizeInventory(); i++) {
            ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);
            if(stack != null && !isUseless(stack)) return false;
        }
        return true;
    }

    private boolean isUseless(ItemStack stack) {
        if(!filter.isEnabled()) return false;
        return invManager.isUseless(stack);
    }

    private boolean isGUI() {
        for(double x = mc.thePlayer.posX - 5; x <= mc.thePlayer.posX + 5; x++) {
            for(double y = mc.thePlayer.posY - 5; y <= mc.thePlayer.posY + 5; y++) {
                for(double z = mc.thePlayer.posZ - 5; z <= mc.thePlayer.posZ + 5; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    if(block instanceof BlockChest || block instanceof BlockEnderChest) return false;
                }
            }
        }
        return true;
    }

    private class StealParticle {
        float x, y;
        float vx, vy;
        int age;
        float targetX, targetY;

        public StealParticle() {
            try {
                ScaledResolution sr = new ScaledResolution(mc);
                float centerX = sr.getScaledWidth() / 2;
                float centerY = sr.getScaledHeight() / 2;

                double angle = Math.random() * Math.PI * 2;
                double distance = 60 + Math.random() * 40;

                this.x = (float)(centerX + Math.cos(angle) * distance);
                this.y = (float)(centerY + Math.sin(angle) * distance);

                this.targetX = (float)(centerX + (Math.random() - 0.5) * 200);
                this.targetY = (float)(centerY + (Math.random() - 0.5) * 200 - 50);

                this.vx = (targetX - x) / 40;
                this.vy = (targetY - y) / 40;
                this.age = 0;
            } catch(Exception e) {
                this.x = 0;
                this.y = 0;
                this.vx = 0;
                this.vy = 0;
                this.age = 0;
            }
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.1f;
            age++;
        }
    }
}