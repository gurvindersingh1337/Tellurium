package vestige.module.impl.combat;

import net.minecraft.network.play.server.S12PacketEntityVelocity;
import vestige.Vestige;
import vestige.event.Listener;
import vestige.event.impl.PacketReceiveEvent;
import vestige.event.impl.PostMotionEvent;
import vestige.event.impl.UpdateEvent;
import vestige.module.Category;
import vestige.module.Module;
import vestige.module.impl.movement.Blink;
import vestige.module.impl.movement.Longjump;
import vestige.module.impl.movement.Speed;
import vestige.setting.impl.IntegerSetting;
import vestige.setting.impl.ModeSetting;
import vestige.util.player.KeyboardUtil;

public class Velocity extends Module {

    public final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "Hypixel", "Packet loss", "Legit");
    private final IntegerSetting horizontal = new IntegerSetting("Horizontal", 0, 0, 100, 2);
    private final IntegerSetting vertical = new IntegerSetting("Vertical", 0, 0, 100, 2);

    private boolean reducing;
    private boolean pendingVelocity;
    private double motionY;
    private int ticks;
    private int offGroundTicks;

    private Blink blinkModule;
    private Longjump longjumpModule;
    private Speed speedModule;

    public Velocity() {
        super("Velocity", Category.COMBAT);
        this.addSettings(mode, horizontal, vertical);
    }

    @Override
    public void onEnable() {
        reducing = false;
        offGroundTicks = 0;
        pendingVelocity = false;
    }

    @Override
    public void onDisable() {
        if (mode.is("Hypixel") && pendingVelocity) {
            pendingVelocity = false;
            mc.thePlayer.motionY = motionY;
            Vestige.instance.getPacketBlinkHandler().stopBlinkingPing();
        }

        if (mode.is("Packet loss")) {
            Vestige.instance.getPacketBlinkHandler().stopAll();
        }
    }

    @Override
    public void onClientStarted() {
        blinkModule = Vestige.instance.getModuleManager().getModule(Blink.class);
        longjumpModule = Vestige.instance.getModuleManager().getModule(Longjump.class);
        speedModule = Vestige.instance.getModuleManager().getModule(Speed.class);
    }

    private boolean canEditVelocity() {
        boolean usingSelfDamageLongjump = longjumpModule.isEnabled() && longjumpModule.mode.is("Self damage");
        return !blinkModule.isEnabled() && !usingSelfDamageLongjump;
    }

    @Listener
    public void onReceive(PacketReceiveEvent event) {
        if (!canEditVelocity()) return;

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = event.getPacket();
            if (packet.getEntityID() != mc.thePlayer.getEntityId()) return;

            switch (mode.getMode()) {
                case "Packet":
                    double hMult = horizontal.getValue() / 100.0;
                    double vMult = vertical.getValue() / 100.0;

                    if (hMult == 0) {
                        event.setCancelled(true);
                        if (vMult > 0) mc.thePlayer.motionY = packet.getMotionY() * vMult / 8000.0;
                    } else {
                        packet.setMotionX((int) (packet.getMotionX() * hMult));
                        packet.setMotionZ((int) (packet.getMotionZ() * hMult));
                        packet.setMotionY((int) (packet.getMotionY() * vMult));
                    }
                    break;

                case "Hypixel":
                    event.setCancelled(true);
                    if (offGroundTicks <= 1 || !speedModule.isEnabled()) {
                        mc.thePlayer.motionY = packet.getMotionY() / 8000.0;
                    } else {
                        pendingVelocity = true;
                        motionY = packet.getMotionY() / 8000.0;
                        Vestige.instance.getPacketBlinkHandler().startBlinkingPing();
                        ticks = 12;
                    }
                    break;

                case "Packet loss":
                    event.setCancelled(true);
                    pendingVelocity = true;
                    break;

                case "Legit":
                    if (mc.currentScreen == null) {
                        mc.gameSettings.keyBindSprint.pressed = true;
                        mc.gameSettings.keyBindForward.pressed = true;
                        mc.gameSettings.keyBindJump.pressed = true;
                        mc.gameSettings.keyBindBack.pressed = false;
                        reducing = true;
                    }
                    break;
            }
        }
    }

    @Listener
    public void onUpdate(UpdateEvent event) {
        if (mc.thePlayer.onGround) offGroundTicks = 0;
        else offGroundTicks++;

        switch (mode.getMode()) {
            case "Hypixel":
                --ticks;
                if (pendingVelocity && (offGroundTicks <= 1 || ticks <= 1)) {
                    pendingVelocity = false;
                    mc.thePlayer.motionY = motionY;
                    Vestige.instance.getPacketBlinkHandler().stopBlinkingPing();
                }
                if (pendingVelocity) mc.gameSettings.keyBindJump.pressed = false;
                break;

            case "Packet loss":
                Vestige.instance.getPacketBlinkHandler().startBlinkingPing();
                if (pendingVelocity) {
                    Vestige.instance.getPacketBlinkHandler().clearPing();
                    pendingVelocity = false;
                } else {
                    Vestige.instance.getPacketBlinkHandler().releasePing();
                }
                break;
        }
    }

    @Listener
    public void onPostMotion(PostMotionEvent event) {
        if (mode.is("Legit") && reducing) {
            if (mc.currentScreen == null) {
                KeyboardUtil.resetKeybindings(mc.gameSettings.keyBindSprint,
                        mc.gameSettings.keyBindForward,
                        mc.gameSettings.keyBindJump,
                        mc.gameSettings.keyBindBack);
            }
            reducing = false;
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}
