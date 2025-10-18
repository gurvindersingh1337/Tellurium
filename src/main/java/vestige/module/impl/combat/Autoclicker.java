package vestige.module.impl.combat;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import vestige.event.Listener;
import vestige.event.impl.RenderEvent;
import vestige.event.impl.TickEvent;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.DoubleSetting;
import vestige.setting.impl.IntegerSetting;
import vestige.setting.impl.ModeSetting;
import vestige.util.misc.TimerUtil;

import java.util.concurrent.ThreadLocalRandom;

public class Autoclicker extends Module {
    private boolean wasHolding;
    private boolean shouldClick;
    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil hitDelay = new TimerUtil();
    private final TimerUtil blockTimer = new TimerUtil();

    private final IntegerSetting minCPS = new IntegerSetting("Min CPS", 10, 1, 25, 1);
    private final IntegerSetting maxCPS = new IntegerSetting("Max CPS", 16, 1, 25, 1);

    private final BooleanSetting triggerbot = new BooleanSetting("Triggerbot", false);
    private final IntegerSetting triggerDelay = new IntegerSetting("Trigger delay", () -> triggerbot.isEnabled(), 50, 0, 500, 10);

    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon only", true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break blocks", false);

    private final BooleanSetting blockHit = new BooleanSetting("Blockhit", false);
    private final IntegerSetting blockChance = new IntegerSetting("Block chance", () -> blockHit.isEnabled(), 85, 0, 100, 5);
    private final IntegerSetting blockDuration = new IntegerSetting("Block duration", () -> blockHit.isEnabled(), 50, 20, 200, 10);

    private final BooleanSetting jitter = new BooleanSetting("Jitter", false);
    private final DoubleSetting jitterAmount = new DoubleSetting("Jitter amount", () -> jitter.isEnabled(), 0.3, 0.1, 2.0, 0.1);

    private final ModeSetting clickMode = new ModeSetting("Click mode", "Normal", "Normal", "Smart", "Burst");
    private final IntegerSetting burstClicks = new IntegerSetting("Burst clicks", () -> clickMode.is("Burst"), 3, 2, 5, 1);
    private final IntegerSetting burstDelay = new IntegerSetting("Burst delay", () -> clickMode.is("Burst"), 500, 200, 1000, 50);

    private final BooleanSetting inventory = new BooleanSetting("Inventory", false);
    private final BooleanSetting rmb = new BooleanSetting("Right click", false);
    private final IntegerSetting rmbMinCPS = new IntegerSetting("RMB min CPS", () -> rmb.isEnabled(), 8, 1, 20, 1);
    private final IntegerSetting rmbMaxCPS = new IntegerSetting("RMB max CPS", () -> rmb.isEnabled(), 12, 1, 20, 1);

    private int burstCount;
    private long burstTime;
    private final TimerUtil rmbTimer = new TimerUtil();

    public Autoclicker() {
        super("Autoclicker", Category.COMBAT);
        this.addSettings(minCPS, maxCPS, triggerbot, triggerDelay, weaponOnly, breakBlocks,
                blockHit, blockChance, blockDuration, jitter, jitterAmount, clickMode, burstClicks,
                burstDelay, inventory, rmb, rmbMinCPS, rmbMaxCPS);
    }

    @Override
    public void onEnable() {
        wasHolding = false;
        burstCount = 0;
        burstTime = 0;
    }

    @Listener
    public void onRender(RenderEvent event) {
        if (triggerbot.isEnabled()) {
            MovingObjectPosition mop = mc.objectMouseOver;
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                Entity entity = mop.entityHit;
                if (entity instanceof EntityLivingBase && !(entity instanceof EntityPlayerSP)) {
                    if (hitDelay.getTimeElapsed() >= triggerDelay.getValue()) {
                        performClick((EntityLivingBase) entity);
                    }
                }
            }
        }

        if (wasHolding && !triggerbot.isEnabled()) {
            if (!canClick()) {
                wasHolding = false;
                return;
            }

            long delay = getClickDelay();

            if (timer.getTimeElapsed() >= delay) {
                shouldClick = true;
                timer.reset();
            }
        }

        if (jitter.isEnabled() && (wasHolding || triggerbot.isEnabled())) {
            double strength = jitterAmount.getValue();
            mc.thePlayer.rotationPitch += (Math.random() - 0.5) * strength;
            mc.thePlayer.rotationYaw += (Math.random() - 0.5) * strength;
        }

        if (rmb.isEnabled() && Mouse.isButtonDown(1)) {
            long rmbMax = (long) (1000.0 / rmbMinCPS.getValue());
            long rmbMin = (long) (1000.0 / rmbMaxCPS.getValue());
            long rmbDelay = rmbMax > rmbMin ? ThreadLocalRandom.current().nextLong(rmbMin, rmbMax) : rmbMin;

            if (rmbTimer.getTimeElapsed() >= rmbDelay) {
                mc.rightClickMouse();
                rmbTimer.reset();
            }
        }
    }

    private long getClickDelay() {
        if (clickMode.is("Burst")) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - burstTime > burstDelay.getValue()) {
                burstCount = 0;
                burstTime = currentTime;
            }

            if (burstCount < burstClicks.getValue()) {
                burstCount++;
                return ThreadLocalRandom.current().nextLong(20, 40);
            } else {
                return burstDelay.getValue();
            }
        }

        long maxDelay = (long) (1000.0 / minCPS.getValue());
        long minDelay = (long) (1000.0 / maxCPS.getValue());

        if (clickMode.is("Smart")) {
            double variance = 0.15;
            long avgDelay = (maxDelay + minDelay) / 2;
            long range = (long) (avgDelay * variance);
            return ThreadLocalRandom.current().nextLong(avgDelay - range, avgDelay + range + 1);
        }

        return maxDelay > minDelay ? ThreadLocalRandom.current().nextLong(minDelay, maxDelay) : minDelay;
    }

    private void performClick(EntityLivingBase target) {
        if (blockHit.isEnabled() && mc.thePlayer.getHeldItem() != null &&
                mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {

            if (ThreadLocalRandom.current().nextInt(100) < blockChance.getValue()) {
                if (blockTimer.getTimeElapsed() >= blockDuration.getValue()) {
                    mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                    blockTimer.reset();
                }
            }
        }

        mc.leftClickCounter = 0;
        mc.thePlayer.swingItem();

        if (target != null) {
            mc.playerController.attackEntity(mc.thePlayer, target);
        }

        hitDelay.reset();
    }

    private boolean canClick() {
        if (!inventory.isEnabled() && mc.currentScreen != null) {
            return false;
        }

        if (weaponOnly.isEnabled()) {
            if (mc.thePlayer.getHeldItem() == null) return false;
            if (!(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) &&
                    !(mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemAxe)) {
                if (!breakBlocks.isEnabled() || !(mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemTool)) {
                    return false;
                }
            }
        }

        if (!breakBlocks.isEnabled() && mc.objectMouseOver != null &&
                mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }

        return true;
    }

    @Listener
    public void onTick(TickEvent event) {
        if (!triggerbot.isEnabled()) {
            if (Mouse.isButtonDown(0)) {
                if (wasHolding && shouldClick) {
                    MovingObjectPosition mop = mc.objectMouseOver;
                    EntityLivingBase target = null;

                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                        Entity entity = mop.entityHit;
                        if (entity instanceof EntityLivingBase) {
                            target = (EntityLivingBase) entity;
                        }
                    }

                    performClick(target);
                    shouldClick = false;
                }
                wasHolding = true;
            } else {
                wasHolding = false;
            }
        }
    }
}