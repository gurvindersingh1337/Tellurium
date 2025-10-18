package vestige.module.impl.movement;

import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.ModeSetting;
import vestige.setting.impl.BooleanSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;

public class Safewalk extends Module {

    public final BooleanSetting offGround = new BooleanSetting("Offground", false);
    public final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Closet");

    public Safewalk() {
        super("Safewalk", Category.MOVEMENT);
        this.addSettings(mode, offGround);
    }

    public void onUpdate() {
        if (mode.is("Normal")) {
            if (!offGround.isEnabled() && mc.thePlayer.onGround) {
                double x = mc.thePlayer.posX;
                double y = mc.thePlayer.posY - 1;
                double z = mc.thePlayer.posZ;
                BlockPos pos = new BlockPos(x, y, z);
                if (mc.theWorld.isAirBlock(pos)) mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
            }
        } else if (mode.is("Closet")) {
            if (mc.thePlayer.onGround && !mc.thePlayer.isSneaking()) {
                double x = mc.thePlayer.posX;
                double y = mc.thePlayer.posY - 1;
                double z = mc.thePlayer.posZ;
                BlockPos pos = new BlockPos(x, y, z);
                if (mc.theWorld.isAirBlock(pos)) mc.thePlayer.setSneaking(true);
                else mc.thePlayer.setSneaking(false);
            }
        }
    }
}
