package vestige.module.impl.movement;

import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import vestige.event.Listener;
import vestige.event.impl.*;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.DoubleSetting;
import vestige.setting.impl.ModeSetting;
import vestige.util.player.MovementUtil;
import vestige.util.player.MoveUtil;

public class Fly extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Myau", "Mospixel");

    private final DoubleSetting vanillaSpeed = new DoubleSetting("Vanilla speed", () -> mode.is("Vanilla"), 2, 0.2, 9, 0.2);
    private final DoubleSetting vanillaVerticalSpeed = new DoubleSetting("Vanilla vertical speed", () -> mode.is("Vanilla"), 2, 0.2, 9, 0.2);

    private final DoubleSetting myauHSpeed = new DoubleSetting("Myau horizontal speed", () -> mode.is("Myau"), 1, 0, 100, 0.1);
    private final DoubleSetting myauVSpeed = new DoubleSetting("Myau vertical speed", () -> mode.is("Myau"), 1, 0, 100, 0.1);

    private double verticalMotion;
    private boolean takingVelocity;
    private double speed;
    private int stage = 0;

    public Fly() {
        super("Fly", Category.MOVEMENT);
        this.addSettings(mode, vanillaSpeed, vanillaVerticalSpeed, myauHSpeed, myauVSpeed);
    }

    @Override
    public void onEnable() {
        verticalMotion = 0;
        takingVelocity = false;
        speed = 0;
        stage = 0;
    }

    @Override
    public void onDisable() {
        mc.thePlayer.capabilities.isFlying = false;
        MovementUtil.strafe(0);
        mc.thePlayer.motionY = 0;
        mc.timer.timerSpeed = 1F;
    }

    @Listener
    public void onUpdate(UpdateEvent event) {
        if(mode.is("Myau")) {
            verticalMotion = 0;
            if(mc.currentScreen == null) {
                if(mc.gameSettings.keyBindJump.isKeyDown()) verticalMotion += myauVSpeed.getValue() * 0.42;
                if(mc.gameSettings.keyBindSneak.isKeyDown()) verticalMotion -= myauVSpeed.getValue() * 0.42;
            }
        }
    }

    @Listener
    public void onMove(MoveEvent event) {
        switch(mode.getMode()) {
            case "Vanilla":
                MovementUtil.strafe(event, vanillaSpeed.getValue());
                if(mc.gameSettings.keyBindJump.isKeyDown()) event.setY(vanillaVerticalSpeed.getValue());
                else if(mc.gameSettings.keyBindSneak.isKeyDown()) event.setY(-vanillaVerticalSpeed.getValue());
                else event.setY(0);
                mc.thePlayer.motionY = 0;
                break;
            case "Myau":
                if(mc.thePlayer.posY % 1.0 != 0.0) mc.thePlayer.motionY = verticalMotion;
                MovementUtil.strafe(event, 0.2873 * myauHSpeed.getValue());
                break;
            case "Mospixel":
                boolean bwffa = false;
                boolean sw = false;
                if(!MoveUtil.isMoving() || mc.thePlayer.isCollidedHorizontally) stage = -1;
                switch(stage) {
                    case -2:
                        try {
                            java.lang.reflect.Field field = mc.thePlayer.getClass().getSuperclass().getDeclaredField("isInWeb");
                            field.setAccessible(true);
                            field.setBoolean(mc.thePlayer, true);
                        } catch (Exception ignored) {}
                        MoveUtil.strafe(1);
                        mc.timer.timerSpeed = 1.67f;
                        break;
                    case -1:
                        mc.thePlayer.motionY = 0;
                        if(mc.thePlayer.onGround) mc.thePlayer.jump();
                    case 0:
                        speed = 0.3;
                        break;
                    case 1:
                        if(bwffa || sw) mc.timer.timerSpeed = 11;
                        if(mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                            speed *= 2.14;
                        }
                        break;
                    case 2:
                        speed = 1;
                        break;
                    case 4:
                        if(sw) mc.timer.timerSpeed = 1.05f;
                        speed -= speed / 109;
                        mc.thePlayer.motionY = 0;
                        break;
                    case 20:
                        if(bwffa) mc.timer.timerSpeed = 10;
                        speed -= speed / 109;
                        mc.thePlayer.motionY = 0;
                        break;
                    case 30:
                        if(bwffa) mc.timer.timerSpeed = 8;
                        speed -= speed / 109;
                        mc.thePlayer.motionY = 0;
                        break;
                    case 50:
                        if(bwffa) mc.timer.timerSpeed = 1.4f;
                        speed -= speed / 109;
                        mc.thePlayer.motionY = 0;
                        break;
                    default:
                        speed -= speed / 109;
                        mc.thePlayer.motionY = 0;
                        break;
                }
                if(!mc.thePlayer.onGround) {
                    if(mc.thePlayer.ticksExisted % 2 == 0) mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 1e-9, mc.thePlayer.posZ);
                    else mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 1e-9, mc.thePlayer.posZ);
                }
                mc.thePlayer.jumpMovementFactor = 0F;
                if(stage != -2) {
                    if(stage != -1) MoveUtil.strafe(Math.max(speed, MoveUtil.getAllowedHorizontalDistance()));
                    else MoveUtil.strafe(MoveUtil.getAllowedHorizontalDistance() - 0.02);
                }
                if(stage != -1 && stage != -2) stage++;
                break;
        }
        takingVelocity = false;
    }

    @Listener
    public void onMotion(MotionEvent event) {}

    @Listener
    public void onPostMotion(PostMotionEvent event) {}

    @Listener
    public void onReceive(PacketReceiveEvent event) {
        if(event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = event.getPacket();
            if(mc.thePlayer.getEntityId() == packet.getEntityID()) takingVelocity = true;
        } else if(event.getPacket() instanceof S08PacketPlayerPosLook) {
            if(mode.is("Myau")) this.setEnabled(false);
        }
    }

    @Listener
    public void onEntityAction(EntityActionEvent event) {}

    @Listener
    public void onSend(PacketSendEvent event) {}

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}
