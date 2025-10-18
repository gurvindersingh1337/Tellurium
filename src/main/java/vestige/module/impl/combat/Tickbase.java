package vestige.module.impl.combat;

import vestige.event.Listener;
import vestige.event.impl.PostMotionEvent;
import vestige.event.impl.RenderEvent;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.IntegerSetting;

public class Tickbase extends Module {

    private int counter = -1;
    public boolean freezing;

    private final IntegerSetting ticks = new IntegerSetting("Ticks", 3, 1, 10, 1);

    public Tickbase() {
        super("Tickbase", Category.COMBAT);
        this.addSettings(ticks);
    }

    @Override
    public void onEnable() {
        counter = -1;
        freezing = false;
    }

    @Override
    public void onDisable() {

    }

    public int getExtraTicks() {
        if (counter-- > 0) {
            return -1;
        } else {
            freezing = false;
        }
        return 0;
    }

    @Listener
    public void onPostMotion(PostMotionEvent event) {
        if (freezing) {
            mc.thePlayer.posX = mc.thePlayer.lastTickPosX;
            mc.thePlayer.posY = mc.thePlayer.lastTickPosY;
            mc.thePlayer.posZ = mc.thePlayer.lastTickPosZ;
        }
    }

    @Listener
    public void onRender(RenderEvent event) {
        if (freezing) {
            mc.timer.renderPartialTicks = 0F;
        }
    }
}
