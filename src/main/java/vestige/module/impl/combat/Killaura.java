package vestige.module.impl.combat;

import lombok.Getter;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings.GameType;
import vestige.Vestige;
import vestige.event.Listener;
import vestige.event.impl.*;
import vestige.module.Category;
import vestige.module.Module;
import vestige.module.impl.movement.Speed;
import vestige.module.impl.player.Antivoid;
import vestige.module.impl.world.AutoBridge;
import vestige.module.impl.world.Scaffold;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.DoubleSetting;
import vestige.setting.impl.IntegerSetting;
import vestige.setting.impl.ModeSetting;
import vestige.util.misc.TimerUtil;
import vestige.util.network.PacketUtil;
import vestige.util.player.FixedRotations;
import vestige.util.player.MovementUtil;
import vestige.util.player.RotationsUtil;

import java.util.ArrayList;
import java.util.Comparator;

public class Killaura extends Module {

    @Getter
    private EntityLivingBase target;

    private int switchTick = 0;
    private boolean hitRegistered = false;
    private boolean blockingState = false;
    private boolean isBlocking = false;
    private boolean fakeBlockState = false;
    private long attackDelayMS = 0L;
    private int blockTick = 0;

    private FixedRotations fixedRotations;
    private double random;
    private double rotSpeed;
    private boolean done;

    public final ModeSetting mode = new ModeSetting("Mode", "Single", "Single", "Switch");
    private final ModeSetting sort = new ModeSetting("Sort", "Distance", "Distance", "Health", "Hurt Time", "FOV");
    private final ModeSetting rotations = new ModeSetting("Rotations", "Normal", "Normal", "Randomised", "Smooth", "None");
    private final DoubleSetting randomAmount = new DoubleSetting("Random amount", () -> rotations.is("Randomised"), 4, 0.25, 10, 0.25);

    public final DoubleSetting startingRange = new DoubleSetting("Starting range", 4, 3, 6, 0.05);
    public final DoubleSetting swingRange = new DoubleSetting("Swing range", 3.5, 3, 6, 0.05);
    public final DoubleSetting attackRange = new DoubleSetting("Attack range", 3, 3, 6, 0.05);
    public final DoubleSetting rotationRange = new DoubleSetting("Rotation range", 4, 3, 6, 0.05);

    private final IntegerSetting fov = new IntegerSetting("FOV", 360, 30, 360, 1);
    private final IntegerSetting minAPS = new IntegerSetting("Min APS", 14, 1, 20, 1);
    private final IntegerSetting maxAPS = new IntegerSetting("Max APS", 14, 1, 20, 1);
    private final IntegerSetting switchDelay = new IntegerSetting("Switch delay", 150, 0, 1000, 1);
    private final IntegerSetting hurtTime = new IntegerSetting("Hurt time", 10, 0, 10, 1);

    public final ModeSetting autoblock = new ModeSetting("Autoblock", "Spoof", "None", "Vanilla", "Spoof", "Hypixel", "Blink", "Interact", "Swap", "Legit", "Fake");
    private final BooleanSetting autoBlockRequirePress = new BooleanSetting("Autoblock require press", false);
    private final DoubleSetting autoBlockAPS = new DoubleSetting("Autoblock APS", 10, 1, 20, 0.5);
    private final DoubleSetting autoBlockRange = new DoubleSetting("Autoblock range", 6, 3, 8, 0.05);

    private final ModeSetting moveFix = new ModeSetting("Move fix", "Silent", "Disabled", "Normal", "Silent", "Strict");
    private final IntegerSetting smoothing = new IntegerSetting("Smoothing", 0, 0, 100, 1);
    private final IntegerSetting angleStep = new IntegerSetting("Angle step", 90, 30, 180, 1);

    private final BooleanSetting throughWalls = new BooleanSetting("Through walls", true);
    private final BooleanSetting requirePress = new BooleanSetting("Require press", false);
    private final BooleanSetting allowMining = new BooleanSetting("Allow mining", true);
    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons only", true);
    private final BooleanSetting allowTools = new BooleanSetting("Allow tools", false);
    private final BooleanSetting whileInventoryOpened = new BooleanSetting("Inventory check", true);
    private final BooleanSetting botCheck = new BooleanSetting("Bot check", true);

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting bosses = new BooleanSetting("Bosses", false);
    private final BooleanSetting monsters = new BooleanSetting("Monsters", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);
    private final BooleanSetting golems = new BooleanSetting("Golems", false);
    private final BooleanSetting silverfish = new BooleanSetting("Silverfish", false);
    private final BooleanSetting teams = new BooleanSetting("Teams", true);
    private final BooleanSetting invisibles = new BooleanSetting("Invisibles", false);
    private final BooleanSetting attackDead = new BooleanSetting("Attack dead", false);

    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil attackTimer = new TimerUtil();

    private Antibot antibotModule;
    private Teams teamsModule;
    private Speed speedModule;
    private Scaffold scaffoldModule;
    private AutoBridge autoBridgeModule;
    private Antivoid antivoidModule;

    public Killaura() {
        super("Killaura", Category.COMBAT);
        this.addSettings(mode, sort, rotations, randomAmount, startingRange, swingRange, attackRange, rotationRange,
                fov, minAPS, maxAPS, switchDelay, hurtTime, autoblock, autoBlockRequirePress, autoBlockAPS, autoBlockRange,
                moveFix, smoothing, angleStep, throughWalls, requirePress, allowMining, weaponsOnly, allowTools,
                whileInventoryOpened, botCheck, players, bosses, monsters, animals, golems, silverfish, teams,
                invisibles, attackDead);
    }

    @Override
    public void onEnable() {
        fixedRotations = new FixedRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        rotSpeed = 15;
        done = false;
        random = 0.5;
        target = null;
        switchTick = 0;
        hitRegistered = false;
        attackDelayMS = 0L;
        blockTick = 0;
        timer.reset();
        attackTimer.reset();
    }

    @Override
    public void onDisable() {
        if(mc.thePlayer != null) {
            stopTargeting();
        }
    }

    @Override
    public void onClientStarted() {
        antibotModule = Vestige.instance.getModuleManager().getModule(Antibot.class);
        teamsModule = Vestige.instance.getModuleManager().getModule(Teams.class);
        speedModule = Vestige.instance.getModuleManager().getModule(Speed.class);
        scaffoldModule = Vestige.instance.getModuleManager().getModule(Scaffold.class);
        autoBridgeModule = Vestige.instance.getModuleManager().getModule(AutoBridge.class);
        antivoidModule = Vestige.instance.getModuleManager().getModule(Antivoid.class);
    }

    private long getAttackDelay() {
        if(isBlocking) {
            return (long)(1000.0 / autoBlockAPS.getValue());
        }
        int minCPS = minAPS.getValue();
        int maxCPS = maxAPS.getValue();
        long delay1 = 1000L / minCPS;
        long delay2 = 1000L / maxCPS;
        return (long)(delay2 + (delay1 - delay2) * random);
    }

    @Listener
    public void onRender(RenderEvent event) {
        if(mc.thePlayer == null || mc.thePlayer.ticksExisted < 10) {
            this.setEnabled(false);
            return;
        }

        if(target != null && attackTimer.getTimeElapsed() >= getAttackDelay()) {
            attackDelayMS = 0L;
        }
    }

    @Listener
    public void onTick(TickEvent event) {
        if(mc.thePlayer.ticksExisted < 10) {
            this.setEnabled(false);
            return;
        }

        random = Math.random();

        if(attackDelayMS > 0L) {
            attackDelayMS -= 50L;
        }

        EntityLivingBase previousTarget = target;

        if(target == null || !isValidTarget(target) ||
                !isBoxInAttackRange(getEntityBox(target)) ||
                !isBoxInSwingRange(getEntityBox(target)) ||
                timer.getTimeElapsed() >= switchDelay.getValue()) {
            timer.reset();

            ArrayList<EntityLivingBase> targets = new ArrayList<>();
            for(Entity entity : mc.theWorld.loadedEntityList) {
                if(entity instanceof EntityLivingBase &&
                        isValidTarget((EntityLivingBase)entity) &&
                        isInRange((EntityLivingBase)entity)) {
                    targets.add((EntityLivingBase)entity);
                }
            }

            if(targets.isEmpty()) {
                target = null;
            } else {
                if(targets.stream().anyMatch(this::isInSwingRange)) {
                    targets.removeIf(e -> !isInSwingRange(e));
                }
                if(targets.stream().anyMatch(this::isInAttackRange)) {
                    targets.removeIf(e -> !isInAttackRange(e));
                }

                targets.sort((e1, e2) -> {
                    int sortBase = 0;
                    switch(sort.getMode()) {
                        case "Health":
                            sortBase = Float.compare(e1.getHealth(), e2.getHealth());
                            break;
                        case "Hurt Time":
                            sortBase = Integer.compare(e1.hurtResistantTime, e2.hurtResistantTime);
                            break;
                        case "FOV":
                            sortBase = Float.compare(getAngleToEntity(e1), getAngleToEntity(e2));
                            break;
                    }
                    return sortBase != 0 ? sortBase :
                            Double.compare(getDistanceToEntity(e1), getDistanceToEntity(e2));
                });

                if(mode.is("Switch") && hitRegistered) {
                    hitRegistered = false;
                    switchTick++;
                }
                if(mode.is("Single") || switchTick >= targets.size()) {
                    switchTick = 0;
                }
                target = targets.get(switchTick);
            }
        }

        getRotations();

        boolean inventoryOpened = mc.currentScreen instanceof GuiContainer && !whileInventoryOpened.isEnabled();
        boolean scaffoldEnabled = (scaffoldModule.isEnabled() || autoBridgeModule.isEnabled());

        if(target == null || inventoryOpened || scaffoldEnabled) {
            stopTargeting();
            return;
        }

        boolean attack = canAttack();
        boolean block = attack && canAutoBlock();

        if(!block) {
            isBlocking = false;
            fakeBlockState = false;
            blockTick = 0;
        }

        if(attack) {
            boolean swap = false;
            boolean attacked = false;

            if(block) {
                swap = handleAutoblock();
            }

            if(isBoxInSwingRange(getEntityBox(target))) {
                updateRotations();

                if(attack && target.hurtTime <= hurtTime.getValue()) {
                    attacked = performAttack();
                }
            }

            if(swap && attacked) {
                interactAttack();
            }
        }
    }

    private boolean handleAutoblock() {
        boolean swap = false;
        switch(autoblock.getMode()) {
            case "Vanilla":
                if(hasValidTarget()) {
                    if(!isPlayerBlocking()) {
                        sendUseItem();
                        swap = true;
                    }
                    isBlocking = true;
                    fakeBlockState = false;
                }
                break;
            case "Spoof":
                if(hasValidTarget()) {
                    int item = mc.thePlayer.inventory.currentItem;
                    if(blockTick == 0 && attackDelayMS <= 50L) {
                        int slot = findEmptySlot(item);
                        PacketUtil.sendPacket(new C09PacketHeldItemChange(slot));
                        PacketUtil.sendPacket(new C09PacketHeldItemChange(item));
                        sendUseItem();
                        swap = true;
                        blockTick = 1;
                    }
                    isBlocking = true;
                    fakeBlockState = false;
                }
                break;
            case "Hypixel":
                if(hasValidTarget()) {
                    switch(blockTick) {
                        case 0:
                            if(!isPlayerBlocking()) {
                                sendUseItem();
                                swap = true;
                            }
                            blockTick = 1;
                            break;
                        case 1:
                            if(isPlayerBlocking()) {
                                stopBlock();
                            }
                            if(attackDelayMS <= 50L) {
                                blockTick = 0;
                            }
                            break;
                    }
                    isBlocking = true;
                    fakeBlockState = true;
                }
                break;
            case "Fake":
                fakeBlockState = hasValidTarget();
                break;
        }
        return swap;
    }

    private void updateRotations() {
        if(rotations.is("Normal") || rotations.is("Randomised") || rotations.is("Smooth")) {
            float[] rots = getRotationsToBox(getEntityBox(target));
            fixedRotations.updateRotations(rots[0], rots[1]);
        }
    }

    private boolean performAttack() {
        if(attackDelayMS > 0L) {
            return false;
        }

        attackDelayMS = getAttackDelay();
        attackTimer.reset();

        mc.thePlayer.swingItem();

        PacketUtil.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
        if(mc.playerController.getCurrentGameType() != GameType.SPECTATOR) {
            mc.thePlayer.attackTargetEntityWithCurrentItem(target);
        }

        hitRegistered = true;
        return true;
    }

    private void sendUseItem() {
        startBlock(mc.thePlayer.getHeldItem());
    }

    private void startBlock(ItemStack itemStack) {
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(itemStack));
        mc.thePlayer.setItemInUse(itemStack, itemStack.getMaxItemUseDuration());
        blockingState = true;
    }

    private void stopBlock() {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                new BlockPos(0, 0, 0),
                EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        blockingState = false;
    }

    private void interactAttack() {
        if(target != null) {
            float[] rots = getRotationsToBox(getEntityBox(target));
            Vec3 hitVec = getHitVec(target);
            if(hitVec != null) {
                PacketUtil.sendPacket(new C02PacketUseEntity(target,
                        new Vec3(hitVec.xCoord - target.posX,
                                hitVec.yCoord - target.posY,
                                hitVec.zCoord - target.posZ)));
                PacketUtil.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.INTERACT));
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(),
                        mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                blockingState = true;
            }
        }
    }

    private boolean canAttack() {
        if(whileInventoryOpened.isEnabled() && mc.currentScreen instanceof GuiContainer) {
            return false;
        }
        if(weaponsOnly.isEnabled() && !isHoldingSword() &&
                (!allowTools.isEnabled() || !isHoldingTool())) {
            return false;
        }
        if(requirePress.isEnabled() && !mc.gameSettings.keyBindAttack.isKeyDown()) {
            return false;
        }
        return true;
    }

    private boolean canAutoBlock() {
        if(!isHoldingSword()) {
            return false;
        }
        return !autoBlockRequirePress.isEnabled() || mc.gameSettings.keyBindUseItem.isKeyDown();
    }

    private boolean hasValidTarget() {
        return mc.theWorld.loadedEntityList.stream()
                .anyMatch(e -> e instanceof EntityLivingBase &&
                        isValidTarget((EntityLivingBase)e) &&
                        isInBlockRange((EntityLivingBase)e));
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if(!mc.theWorld.loadedEntityList.contains(entity)) return false;
        if(entity == mc.thePlayer || entity == mc.thePlayer.ridingEntity) return false;
        if(entity.deathTime > 0 && !attackDead.isEnabled()) return false;
        if(getAngleToEntity(entity) > fov.getValue()) return false;
        if(!throughWalls.isEnabled() && !canSeeEntity(entity)) return false;
        if((entity.isInvisible() || entity.isInvisibleToPlayer(mc.thePlayer)) && !invisibles.isEnabled()) return false;

        if(entity instanceof EntityPlayer) {
            if(!players.isEnabled()) return false;
            if(!teamsModule.canAttack((EntityPlayer)entity)) return false;
            if(botCheck.isEnabled() && !antibotModule.canAttack(entity, this)) return false;
            return true;
        }

        if(entity instanceof EntityDragon || entity instanceof EntityWither) {
            return bosses.isEnabled();
        }

        if(entity instanceof EntityMob || entity instanceof EntitySlime) {
            if(entity instanceof EntitySilverfish) {
                return silverfish.isEnabled();
            }
            return monsters.isEnabled();
        }

        if(entity instanceof EntityAnimal || entity instanceof EntityBat ||
                entity instanceof EntitySquid || entity instanceof EntityVillager) {
            return animals.isEnabled();
        }

        if(entity instanceof EntityIronGolem) {
            return golems.isEnabled();
        }

        return false;
    }

    private boolean isInRange(EntityLivingBase entity) {
        return isInBlockRange(entity) || isInSwingRange(entity) || isInAttackRange(entity);
    }

    private boolean isInBlockRange(EntityLivingBase entity) {
        return getDistanceToEntity(entity) <= autoBlockRange.getValue();
    }

    private boolean isInSwingRange(EntityLivingBase entity) {
        return getDistanceToEntity(entity) <= swingRange.getValue();
    }

    private boolean isBoxInSwingRange(AxisAlignedBB box) {
        return getDistanceToBox(box) <= swingRange.getValue();
    }

    private boolean isInAttackRange(EntityLivingBase entity) {
        return getDistanceToEntity(entity) <= attackRange.getValue();
    }

    private boolean isBoxInAttackRange(AxisAlignedBB box) {
        return getDistanceToBox(box) <= attackRange.getValue();
    }

    private int findEmptySlot(int currentSlot) {
        for(int i = 0; i < 9; i++) {
            if(i != currentSlot && mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return i;
            }
        }
        return Math.floorMod(currentSlot - 1, 9);
    }

    public boolean isBlocking() {
        return fakeBlockState && isHoldingSword();
    }

    private boolean isPlayerBlocking() {
        return (mc.thePlayer.isUsingItem() || blockingState) && isHoldingSword();
    }

    private boolean isHoldingSword() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        return stack != null && stack.getItem() instanceof ItemSword;
    }

    private boolean isHoldingTool() {
        return false;
    }

    private void stopTargeting() {
        target = null;
        releaseBlocking();
        switchTick = 0;
        hitRegistered = false;
        attackDelayMS = 0L;
        blockTick = 0;
    }

    private void releaseBlocking() {
        if(blockingState) {
            stopBlock();
        }
        blockingState = false;
        isBlocking = false;
        fakeBlockState = false;
    }

    private void getRotations() {
        float yaw = fixedRotations.getYaw();
        float pitch = fixedRotations.getPitch();

        if(target != null) {
            float[] rots = RotationsUtil.getRotationsToEntity(target, false);

            switch(rotations.getMode()) {
                case "Normal":
                    yaw = rots[0];
                    pitch = rots[1];
                    break;
                case "Randomised":
                    double amount = randomAmount.getValue();
                    yaw = (float)(rots[0] + Math.random() * amount - amount / 2);
                    pitch = (float)(rots[1] + Math.random() * amount - amount / 2);
                    break;
                case "Smooth":
                    float yaw1 = rots[0];
                    float currentYaw = MathHelper.wrapAngleTo180_float(yaw);
                    float diff = Math.abs(currentYaw - yaw1);

                    if(diff >= 8) {
                        if(diff > 35) {
                            rotSpeed += 4 - Math.random();
                            rotSpeed = Math.max(rotSpeed, 31 - Math.random());
                        } else {
                            rotSpeed -= 6.5 - Math.random();
                            rotSpeed = Math.max(rotSpeed, 14 - Math.random());
                        }

                        if(diff <= 180) {
                            yaw += currentYaw > yaw1 ? -rotSpeed : rotSpeed;
                        } else {
                            yaw += currentYaw > yaw1 ? rotSpeed : -rotSpeed;
                        }
                    } else {
                        yaw += currentYaw > yaw1 ? -diff * 0.8 : diff * 0.8;
                    }

                    yaw += Math.random() * 0.7 - 0.35;
                    pitch = (float)(mc.thePlayer.rotationPitch +
                            (rots[1] - mc.thePlayer.rotationPitch) * 0.6);
                    pitch += Math.random() * 0.5 - 0.25;
                    done = false;
                    break;
            }
        } else {
            if(rotations.is("Smooth")) {
                rotSpeed = 15;
                done = true;
            }
        }

        fixedRotations.updateRotations(yaw, pitch);
    }

    @Listener
    public void onMotion(MotionEvent event) {
        if(!rotations.is("None") && target != null) {
            event.setYaw(fixedRotations.getYaw());
            event.setPitch(fixedRotations.getPitch());
        }
    }

    @Listener
    public void onStrafe(StrafeEvent event) {
        if(!rotations.is("None") && target != null) {
            if(moveFix.is("Normal")) {
                event.setYaw(fixedRotations.getYaw());
            } else if(moveFix.is("Silent")) {
                event.setYaw(fixedRotations.getYaw());
                float diff = MathHelper.wrapAngleTo180_float(
                        MathHelper.wrapAngleTo180_float(fixedRotations.getYaw()) -
                                MathHelper.wrapAngleTo180_float(MovementUtil.getPlayerDirection())) + 22.5F;
                if(diff < 0) diff = 360 + diff;

                int a = (int)(diff / 45.0);
                float value = event.getForward() != 0 ? Math.abs(event.getForward()) : Math.abs(event.getStrafe());
                float forward = value;
                float strafe = 0;

                for(int i = 0; i < 8 - a; i++) {
                    float[] dirs = MovementUtil.incrementMoveDirection(forward, strafe);
                    forward = dirs[0];
                    strafe = dirs[1];
                }

                event.setForward(forward);
                event.setStrafe(strafe);
            }
        }
    }

    @Listener
    public void onJump(JumpEvent event) {
        if(target != null && !rotations.is("None") && !moveFix.is("Disabled")) {
            event.setYaw(fixedRotations.getYaw());
        }
    }

    @Listener
    public void onItemRender(ItemRenderEvent event) {
        if((fakeBlockState || isPlayerBlocking()) && !autoblock.is("None")) {
            event.setRenderBlocking(true);
        }
    }

    private AxisAlignedBB getEntityBox(EntityLivingBase entity) {
        double size = entity.getCollisionBorderSize();
        return entity.getEntityBoundingBox().expand(size, size, size);
    }

    private float[] getRotationsToBox(AxisAlignedBB box) {
        return RotationsUtil.getRotationsToEntity(target, false);
    }

    private Vec3 getHitVec(EntityLivingBase entity) {
        return new Vec3(entity.posX, entity.posY + entity.getEyeHeight() * 0.9, entity.posZ);
    }

    private float getAngleToEntity(EntityLivingBase entity) {
        float[] rots = RotationsUtil.getRotationsToEntity(entity, false);
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rots[0] - mc.thePlayer.rotationYaw));
        float pitchDiff = Math.abs(rots[1] - mc.thePlayer.rotationPitch);
        return yawDiff + pitchDiff;
    }

    private boolean canSeeEntity(EntityLivingBase entity) {
        return mc.thePlayer.canEntityBeSeen(entity);
    }

    public double getDistanceToEntity(EntityLivingBase entity) {
        Vec3 playerVec = new Vec3(mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ);
        double yDiff = mc.thePlayer.posY - entity.posY;
        double targetY = yDiff > 0 ? entity.posY + entity.getEyeHeight() :
                -yDiff < mc.thePlayer.getEyeHeight() ?
                        mc.thePlayer.posY + mc.thePlayer.getEyeHeight() : entity.posY;
        Vec3 targetVec = new Vec3(entity.posX, targetY, entity.posZ);
        return playerVec.distanceTo(targetVec) - 0.3F;
    }

    private double getDistanceToBox(AxisAlignedBB box) {
        Vec3 playerVec = new Vec3(mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ);
        double x = Math.max(box.minX, Math.min(playerVec.xCoord, box.maxX));
        double y = Math.max(box.minY, Math.min(playerVec.yCoord, box.maxY));
        double z = Math.max(box.minZ, Math.min(playerVec.zCoord, box.maxZ));
        return playerVec.distanceTo(new Vec3(x, y, z));
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}