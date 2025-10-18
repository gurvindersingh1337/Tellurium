package vestige.module.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.DoubleSetting;
import vestige.setting.impl.ModeSetting;

public class Animations extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public final ModeSetting blockMode = new ModeSetting("Block Animation", "1.7", "1.7", "1.8", "Spin", "Smooth", "Swing", "Stab", "Slide", "Push", "Exhibition", "Avatar", "BlockHit");
    public final DoubleSetting swingSlowdown = new DoubleSetting("Swing slowdown", 1, 0.1, 8, 0.1);
    public final DoubleSetting blockX = new DoubleSetting("Block X", 0, -1, 1, 0.01);
    public final DoubleSetting blockY = new DoubleSetting("Block Y", 0, -1, 1, 0.01);
    public final DoubleSetting blockZ = new DoubleSetting("Block Z", 0, -1, 1, 0.01);
    public final DoubleSetting spinSpeed = new DoubleSetting("Spin Speed", 5, 1, 20, 0.5);

    public Animations() {
        super("Animations", Category.VISUAL);
        this.addSettings(blockMode, swingSlowdown, blockX, blockY, blockZ, spinSpeed);
    }

    public void applyBlockAnimation(float swingProgress, float partialTicks) {
        if(!this.isEnabled()) {
            return;
        }

        ItemStack stack = mc.thePlayer.getHeldItem();
        if(stack == null) {
            return;
        }

        String mode = blockMode.getMode();

        float x = (float) blockX.getValue();
        float y = (float) blockY.getValue();
        float z = (float) blockZ.getValue();

        GlStateManager.translate(x, y, z);

        boolean isBlocking = mc.thePlayer.isBlocking() && stack.getItem() instanceof ItemSword;

        if(mode.equals("BlockHit") && isBlocking) {
            doBlockHit(swingProgress);
            return;
        }

        switch(mode) {
            case "1.7":
                do1_7(swingProgress);
                break;
            case "1.8":
                do1_8(swingProgress);
                break;
            case "Spin":
                doSpin((float) spinSpeed.getValue());
                break;
            case "Smooth":
                doSmooth(swingProgress);
                break;
            case "Swing":
                doSwing(swingProgress);
                break;
            case "Stab":
                doStab(swingProgress);
                break;
            case "Slide":
                doSlide(swingProgress);
                break;
            case "Push":
                doPush(swingProgress);
                break;
            case "Exhibition":
                doExhibition(swingProgress);
                break;
            case "Avatar":
                doAvatar(swingProgress);
                break;
        }
    }

    private void do1_7(float swingProgress) {
        GlStateManager.translate(0.0F, 0.2F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(-f * 20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-f1 * 20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-f1 * 80.0F, 1.0F, 0.0F, 0.0F);
    }

    private void do1_8(float swingProgress) {
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-f * 40.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-f1 * 30.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-f1 * 40.0F, 1.0F, 0.0F, 0.0F);
    }

    private void doSpin(float speed) {
        float rotation = (System.currentTimeMillis() % 360000) / 1000.0F * speed;
        GlStateManager.translate(0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rotation * 0.5F, 1.0F, 0.0F, 0.0F);
    }

    private void doSmooth(float swingProgress) {
        float f = MathHelper.sin((float) (swingProgress * Math.PI));
        GlStateManager.translate(0.0F, f * 0.1F, 0.0F);
        GlStateManager.rotate(f * 35.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f * 10.0F, 1.0F, 0.0F, 0.0F);
    }

    private void doSwing(float swingProgress) {
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        GlStateManager.translate(f * 0.3F, 0.0F, 0.0F);
        GlStateManager.rotate(f * 60.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f * 30.0F, 1.0F, 0.0F, 0.0F);
    }

    private void doStab(float swingProgress) {
        float f = 1.0F - swingProgress;
        GlStateManager.translate(0.0F, 0.0F, f * 0.5F);
        GlStateManager.rotate(f * -90.0F, 1.0F, 0.0F, 0.0F);
    }

    private void doSlide(float swingProgress) {
        float f = MathHelper.sin(swingProgress * (float) Math.PI);
        GlStateManager.translate(f * 0.4F, f * 0.2F, 0.0F);
        GlStateManager.rotate(f * 45.0F, 0.0F, 1.0F, 0.0F);
    }

    private void doPush(float swingProgress) {
        float f = 1.0F - swingProgress;
        float f1 = MathHelper.sin(swingProgress * (float) Math.PI);
        GlStateManager.translate(0.0F, f1 * 0.1F, f * 0.3F);
        GlStateManager.rotate(f * -45.0F, 1.0F, 0.0F, 0.0F);
    }

    private void doExhibition(float swingProgress) {
        float f = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.translate(0.0F, 0.1F, 0.0F);
        GlStateManager.rotate(f * -35.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f * -10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f * -50.0F, 0.0F, 1.0F, 0.0F);
    }

    private void doAvatar(float swingProgress) {
        float rotation = (System.currentTimeMillis() % 2000) / 2000.0F * 360.0F;
        float bounce = MathHelper.sin(rotation * (float) Math.PI / 180.0F) * 0.1F;
        GlStateManager.translate(0.3F, bounce + 0.2F, 0.0F);
        GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(15.0F, 1.0F, 0.0F, 0.0F);
    }

    private void doBlockHit(float swingProgress) {
        do1_7(swingProgress);
    }
}