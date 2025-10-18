package vestige.module.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import vestige.Vestige;
import vestige.event.Listener;
import vestige.event.impl.RenderEvent;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.DoubleSetting;
import vestige.setting.impl.IntegerSetting;
import vestige.setting.impl.ModeSetting;
import vestige.util.misc.TimerUtil;
import vestige.util.player.FixedRotations;
import vestige.util.player.RotationsUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class AimAssist extends Module {

    private Antibot antibotModule;
    private Teams teamsModule;
    private Autoclicker autoclickerModule;

    private final ModeSetting filter = new ModeSetting("Filter", "Range", "Range", "Health", "Angle", "Armor");
    private final DoubleSetting range = new DoubleSetting("Range", 4.5, 3, 10, 0.1);

    private final IntegerSetting speed = new IntegerSetting("Speed", 12, 1, 50, 1);
    private final DoubleSetting speedVariation = new DoubleSetting("Speed variation", 0.2, 0, 0.5, 0.05);

    private final BooleanSetting clickOnly = new BooleanSetting("Click only", true);
    private final BooleanSetting autoclickerSync = new BooleanSetting("Autoclicker sync", true);

    private final BooleanSetting vertical = new BooleanSetting("Vertical", true);
    private final IntegerSetting verticalSpeed = new IntegerSetting("Vertical speed", () -> vertical.isEnabled(), 8, 1, 30, 1);

    private final BooleanSetting smooth = new BooleanSetting("Smooth", true);
    private final DoubleSetting smoothing = new DoubleSetting("Smoothing", () -> smooth.isEnabled(), 0.7, 0.1, 1, 0.05);

    private final BooleanSetting fov = new BooleanSetting("FOV check", true);
    private final IntegerSetting fovValue = new IntegerSetting("FOV", () -> fov.isEnabled(), 180, 30, 360, 10);

    private final BooleanSetting strafe = new BooleanSetting("Strafe fix", false);

    private final BooleanSetting randomization = new BooleanSetting("Randomization", true);
    private final DoubleSetting randomStrength = new DoubleSetting("Random strength", () -> randomization.isEnabled(), 0.3, 0.1, 1, 0.05);

    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon only", false);

    private final TimerUtil timer = new TimerUtil();
    private final Random random = new Random();

    private EntityPlayer target;
    private FixedRotations rotations;

    private float lastYawDiff;
    private float lastPitchDiff;

    public AimAssist() {
        super("AimAssist", Category.COMBAT);
        this.addSettings(filter, range, speed, speedVariation, clickOnly, autoclickerSync,
                vertical, verticalSpeed, smooth, smoothing, fov, fovValue, strafe,
                randomization, randomStrength, weaponOnly);
    }

    @Override
    public void onEnable() {
        if(mc.thePlayer != null) {
            rotations = new FixedRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }
        lastYawDiff = 0;
        lastPitchDiff = 0;
    }

    @Override
    public void onClientStarted() {
        antibotModule = Vestige.instance.getModuleManager().getModule(Antibot.class);
        teamsModule = Vestige.instance.getModuleManager().getModule(Teams.class);
        autoclickerModule = Vestige.instance.getModuleManager().getModule(Autoclicker.class);
    }

    @Listener
    public void onRender(RenderEvent event) {
        if(mc.thePlayer == null || mc.theWorld == null || rotations == null) {
            return;
        }

        target = findTarget();

        boolean shouldAim = false;

        if (clickOnly.isEnabled()) {
            shouldAim = Mouse.isButtonDown(0);
        } else {
            shouldAim = true;
        }

        if (autoclickerSync.isEnabled() && autoclickerModule != null && autoclickerModule.isEnabled()) {
            shouldAim = true;
        }

        if (target != null && shouldAim && mc.currentScreen == null) {
            if (weaponOnly.isEnabled()) {
                if (mc.thePlayer.getHeldItem() == null ||
                        !(mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemSword) &&
                                !(mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemAxe)) {
                    return;
                }
            }

            float rots[] = RotationsUtil.getRotationsToEntity(target, false);

            float targetYaw = rots[0];
            float targetPitch = rots[1];
            float currentYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
            float currentPitch = mc.thePlayer.rotationPitch;

            float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);
            float pitchDiff = targetPitch - currentPitch;

            float absYawDiff = Math.abs(yawDiff);
            float absPitchDiff = Math.abs(pitchDiff);

            if (absYawDiff >= 2 && absYawDiff <= 358) {
                float speedMultiplier = (float) (1.0 - speedVariation.getValue() + random.nextDouble() * speedVariation.getValue() * 2);
                float aimSpeed = speed.getValue() * speedMultiplier;

                float acceleration = smooth.isEnabled() ? (float) smoothing.getValue() : 1.0F;

                float yawSpeed;
                if (absYawDiff <= aimSpeed) {
                    yawSpeed = absYawDiff * 0.85F;
                } else {
                    yawSpeed = aimSpeed;
                }

                if (smooth.isEnabled()) {
                    yawSpeed = lastYawDiff + (yawSpeed - lastYawDiff) * acceleration;
                }

                float finalYawSpeed = yawSpeed * Math.max(timer.getTimeElapsed(), 1) * 0.008F;

                if (randomization.isEnabled()) {
                    finalYawSpeed += (float) ((random.nextDouble() - 0.5) * randomStrength.getValue());
                }

                if (yawDiff > 0) {
                    mc.thePlayer.rotationYaw += finalYawSpeed;
                } else {
                    mc.thePlayer.rotationYaw -= finalYawSpeed;
                }

                lastYawDiff = yawSpeed;
            }

            if (vertical.isEnabled() && absPitchDiff >= 1) {
                float vSpeed = verticalSpeed.getValue();

                float pitchSpeed;
                if (absPitchDiff <= vSpeed) {
                    pitchSpeed = absPitchDiff * 0.8F;
                } else {
                    pitchSpeed = vSpeed;
                }

                if (smooth.isEnabled()) {
                    pitchSpeed = lastPitchDiff + (pitchSpeed - lastPitchDiff) * (float) smoothing.getValue();
                }

                float finalPitchSpeed = pitchSpeed * Math.max(timer.getTimeElapsed(), 1) * 0.008F;

                if (randomization.isEnabled()) {
                    finalPitchSpeed += (float) ((random.nextDouble() - 0.5) * randomStrength.getValue() * 0.5);
                }

                if (pitchDiff > 0) {
                    mc.thePlayer.rotationPitch += finalPitchSpeed;
                } else {
                    mc.thePlayer.rotationPitch -= finalPitchSpeed;
                }

                mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90, 90);

                lastPitchDiff = pitchSpeed;
            }
        }

        rotations.updateRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);

        if (!strafe.isEnabled()) {
            mc.thePlayer.rotationYaw = rotations.getYaw();
            mc.thePlayer.rotationPitch = rotations.getPitch();
        }

        timer.reset();
    }

    public EntityPlayer findTarget() {
        if(mc.theWorld == null) {
            return null;
        }

        ArrayList<EntityPlayer> entities = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                EntityPlayer player = (EntityPlayer) entity;

                if (canAttackEntity(player)) {
                    entities.add(player);
                }
            }
        }

        if (entities.size() > 0) {
            switch (filter.getMode()) {
                case "Range":
                    entities.sort(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)));
                    break;
                case "Health":
                    entities.sort(Comparator.comparingDouble(entity -> entity.getHealth()));
                    break;
                case "Angle":
                    entities.sort(Comparator.comparingDouble(entity -> {
                        float[] rots = RotationsUtil.getRotationsToEntity(entity, false);
                        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rots[0] - mc.thePlayer.rotationYaw));
                        return yawDiff;
                    }));
                    break;
                case "Armor":
                    entities.sort(Comparator.comparingInt(entity -> entity.getTotalArmorValue()));
                    break;
            }

            return entities.get(0);
        }

        return null;
    }

    private boolean canAttackEntity(EntityPlayer player) {
        if(teamsModule == null || antibotModule == null) {
            return false;
        }

        if (!player.isDead && player.isEntityAlive()) {
            if (mc.thePlayer.getDistanceToEntity(player) < range.getValue()) {
                if (fov.isEnabled()) {
                    float[] rots = RotationsUtil.getRotationsToEntity(player, false);
                    float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rots[0] - mc.thePlayer.rotationYaw));

                    if (yawDiff > fovValue.getValue() / 2f) {
                        return false;
                    }
                }

                if (!player.isInvisible() && !player.isInvisibleToPlayer(mc.thePlayer)) {
                    if (!teamsModule.canAttack(player)) {
                        return false;
                    }

                    return antibotModule.canAttack(player, this);
                }
            }
        }

        return false;
    }

}