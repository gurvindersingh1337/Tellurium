package vestige.module.impl.combat;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.*;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import vestige.Vestige;
import vestige.event.Listener;
import vestige.event.impl.PacketReceiveEvent;
import vestige.event.impl.PostMotionEvent;
import vestige.event.impl.Render3DEvent;
import vestige.handler.packet.DelayedPacket;
import vestige.module.Category;
import vestige.module.Module;
import vestige.setting.impl.BooleanSetting;
import vestige.setting.impl.DoubleSetting;
import vestige.setting.impl.IntegerSetting;
import vestige.util.player.PendingVelocity;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Backtrack extends Module {

    private final IntegerSetting delay = new IntegerSetting("Delay", 500, 100, 2000, 50);
    private final DoubleSetting minRange = new DoubleSetting("Min range", 2.8, 1, 6, 0.1);

    private final BooleanSetting delayPing = new BooleanSetting("Delay ping", true);
    private final BooleanSetting delayVelocity = new BooleanSetting("Delay velocity", () -> delayPing.isEnabled(), true);
    private final BooleanSetting showESP = new BooleanSetting("Show ESP", true);
    private final BooleanSetting showBreadcrumb = new BooleanSetting("Show Breadcrumb", true);

    private final CopyOnWriteArrayList<DelayedPacket> delayedPackets = new CopyOnWriteArrayList<>();

    private Killaura killauraModule;

    private EntityLivingBase lastTarget;
    private EntityLivingBase lastCursorTarget;

    private int cursorTargetTicks;

    private PendingVelocity lastVelocity;

    private EntityOtherPlayerMP fakePlayer;

    private ArrayList<Vec3> breadcrumbs = new ArrayList<>();

    public Backtrack() {
        super("Backtrack", Category.COMBAT);
        this.addSettings(delay, minRange, delayPing, delayVelocity, showESP, showBreadcrumb);
    }

    @Override
    public void onClientStarted() {
        killauraModule = Vestige.instance.getModuleManager().getModule(Killaura.class);
    }

    @Override
    public void onDisable() {
        clearPackets();
        removeFakePlayer();
        breadcrumbs.clear();
    }

    @Listener
    public void onReceive(PacketReceiveEvent event) {
        if (mc.thePlayer == null || mc.thePlayer.ticksExisted < 5) {
            if (!delayedPackets.isEmpty()) {
                delayedPackets.clear();
            }
        }

        EntityLivingBase currentTarget = getCurrentTarget();

        if (currentTarget == null || currentTarget != lastTarget) {
            clearPackets();
            removeFakePlayer();
            breadcrumbs.clear();
        }

        if (currentTarget != null) {
            if (event.getPacket() instanceof S14PacketEntity) {
                S14PacketEntity packet = event.getPacket();

                if (packet.getEntity(mc.getNetHandler().clientWorldController) == currentTarget) {
                    int x = currentTarget.serverPosX + packet.getX();
                    int y = currentTarget.serverPosY + packet.getY();
                    int z = currentTarget.serverPosZ + packet.getZ();

                    double posX = (double) x / 32.0D;
                    double posY = (double) y / 32.0D;
                    double posZ = (double) z / 32.0D;

                    if (getDistanceToPlayer(posX, posY, posZ, currentTarget.getEyeHeight()) >= minRange.getValue()) {
                        event.setCancelled(true);
                        delayedPackets.add(new DelayedPacket(packet));

                        if (showESP.isEnabled() && fakePlayer == null && currentTarget instanceof EntityPlayer) {
                            spawnFakePlayer(currentTarget);
                        }
                    }
                }
            } else if (event.getPacket() instanceof S18PacketEntityTeleport) {
                S18PacketEntityTeleport packet = event.getPacket();

                if (packet.getEntityId() == currentTarget.getEntityId()) {
                    double d0 = (double) packet.getX() / 32.0D;
                    double d1 = (double) packet.getY() / 32.0D;
                    double d2 = (double) packet.getZ() / 32.0D;

                    if (getDistanceToPlayer(d0, d1, d2, currentTarget.getEyeHeight()) >= minRange.getValue()) {
                        event.setCancelled(true);
                        delayedPackets.add(new DelayedPacket(packet));

                        if (showESP.isEnabled() && fakePlayer == null && currentTarget instanceof EntityPlayer) {
                            spawnFakePlayer(currentTarget);
                        }
                    }
                }
            } else if (event.getPacket() instanceof S32PacketConfirmTransaction || event.getPacket() instanceof S00PacketKeepAlive) {
                if (!delayedPackets.isEmpty() && delayPing.isEnabled()) {
                    event.setCancelled(true);
                    delayedPackets.add(new DelayedPacket(event.getPacket()));
                }
            } else if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = event.getPacket();

                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    if (!delayedPackets.isEmpty() && delayPing.isEnabled() && delayVelocity.isEnabled()) {
                        event.setCancelled(true);
                        lastVelocity = new PendingVelocity(packet.getMotionX() / 8000.0, packet.getMotionY() / 8000.0, packet.getMotionZ() / 8000.0);
                    }
                }
            }
        }

        lastTarget = currentTarget;
    }

    @Listener
    public void onPostMotion(PostMotionEvent event) {
        updatePackets();

        if (showBreadcrumb.isEnabled() && mc.thePlayer != null) {
            Vec3 currentPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

            if (breadcrumbs.isEmpty() || breadcrumbs.get(breadcrumbs.size() - 1).distanceTo(currentPos) > 0.5) {
                breadcrumbs.add(currentPos);
            }

            if (breadcrumbs.size() > 100) {
                breadcrumbs.remove(0);
            }
        }
    }

    @Listener
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (showESP.isEnabled() && fakePlayer != null && !delayedPackets.isEmpty()) {
            try {
                double x = fakePlayer.lastTickPosX + (fakePlayer.posX - fakePlayer.lastTickPosX) * event.getPartialTicks();
                double y = fakePlayer.lastTickPosY + (fakePlayer.posY - fakePlayer.lastTickPosY) * event.getPartialTicks();
                double z = fakePlayer.lastTickPosZ + (fakePlayer.posZ - fakePlayer.lastTickPosZ) * event.getPartialTicks();

                double renderX = x - mc.getRenderManager().viewerPosX;
                double renderY = y - mc.getRenderManager().viewerPosY;
                double renderZ = z - mc.getRenderManager().viewerPosZ;

                GL11.glPushMatrix();
                GL11.glTranslated(renderX, renderY, renderZ);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
                GL11.glLineWidth(2.0F);

                GL11.glColor4f(1.0F, 0.0F, 0.0F, 0.5F);

                drawBoundingBox(fakePlayer.getEntityBoundingBox().offset(-fakePlayer.posX, -fakePlayer.posY, -fakePlayer.posZ));

                GL11.glDepthMask(true);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        if (showBreadcrumb.isEnabled() && !breadcrumbs.isEmpty()) {
            try {
                GL11.glPushMatrix();
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
                GL11.glLineWidth(3.0F);

                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldRenderer = tessellator.getWorldRenderer();

                worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

                for (int i = 0; i < breadcrumbs.size(); i++) {
                    Vec3 pos = breadcrumbs.get(i);
                    double renderX = pos.xCoord - mc.getRenderManager().viewerPosX;
                    double renderY = pos.yCoord - mc.getRenderManager().viewerPosY;
                    double renderZ = pos.zCoord - mc.getRenderManager().viewerPosZ;

                    float alpha = (float) i / breadcrumbs.size();
                    worldRenderer.pos(renderX, renderY, renderZ).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
                }

                tessellator.draw();

                GL11.glDepthMask(true);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glPopMatrix();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void drawBoundingBox(net.minecraft.util.AxisAlignedBB aabb) {
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.minZ);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.minZ);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.minZ);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.maxZ);
        GL11.glEnd();
    }

    private void spawnFakePlayer(EntityLivingBase target) {
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            fakePlayer = new EntityOtherPlayerMP(mc.theWorld, player.getGameProfile());
            fakePlayer.setPosition(target.posX, target.posY, target.posZ);
            fakePlayer.rotationYaw = target.rotationYaw;
            fakePlayer.rotationPitch = target.rotationPitch;
            fakePlayer.rotationYawHead = target.rotationYawHead;
            fakePlayer.inventory = player.inventory;
            fakePlayer.renderYawOffset = player.renderYawOffset;
            mc.theWorld.addEntityToWorld(-12345, fakePlayer);
        }
    }

    private void removeFakePlayer() {
        if (fakePlayer != null) {
            mc.theWorld.removeEntityFromWorld(-12345);
            fakePlayer = null;
        }
    }

    public EntityLivingBase getCurrentTarget() {
        if (killauraModule == null) {
            killauraModule = Vestige.instance.getModuleManager().getModule(Killaura.class);
        }

        if (killauraModule.isEnabled() && killauraModule.getTarget() != null) {
            return killauraModule.getTarget();
        } else if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            lastCursorTarget = (EntityLivingBase) mc.objectMouseOver.entityHit;

            return (EntityLivingBase) mc.objectMouseOver.entityHit;
        } else if (lastCursorTarget != null) {
            if (++cursorTargetTicks > 10) {
                lastCursorTarget = null;
            } else {
                return lastCursorTarget;
            }
        }

        return null;
    }

    public void updatePackets() {
        if (!delayedPackets.isEmpty()) {
            for (DelayedPacket p : delayedPackets) {
                if (p.getTimer().getTimeElapsed() >= delay.getValue()) {
                    clearPackets();
                    removeFakePlayer();

                    if (lastVelocity != null) {
                        mc.thePlayer.motionX = lastVelocity.getX();
                        mc.thePlayer.motionY = lastVelocity.getY();
                        mc.thePlayer.motionZ = lastVelocity.getZ();
                        lastVelocity = null;
                    }

                    return;
                }
            }
        }
    }

    public void clearPackets() {
        if (lastVelocity != null) {
            mc.thePlayer.motionX = lastVelocity.getX();
            mc.thePlayer.motionY = lastVelocity.getY();
            mc.thePlayer.motionZ = lastVelocity.getZ();
            lastVelocity = null;
        }

        if (!delayedPackets.isEmpty()) {
            for (DelayedPacket p : delayedPackets) {
                handlePacket(p.getPacket());
            }
            delayedPackets.clear();
        }
    }

    public void handlePacket(Packet packet) {
        if (packet instanceof S14PacketEntity) {
            handleEntityMovement((S14PacketEntity) packet);
        } else if (packet instanceof S18PacketEntityTeleport) {
            handleEntityTeleport((S18PacketEntityTeleport) packet);
        } else if (packet instanceof S32PacketConfirmTransaction) {
            handleConfirmTransaction((S32PacketConfirmTransaction) packet);
        } else if (packet instanceof S00PacketKeepAlive) {
            mc.getNetHandler().handleKeepAlive((S00PacketKeepAlive) packet);
        }
    }

    public void handleEntityMovement(S14PacketEntity packetIn) {
        Entity entity = packetIn.getEntity(mc.getNetHandler().clientWorldController);

        if (entity != null) {
            entity.serverPosX += packetIn.getX();
            entity.serverPosY += packetIn.getY();
            entity.serverPosZ += packetIn.getZ();
            double d0 = (double) entity.serverPosX / 32.0D;
            double d1 = (double) entity.serverPosY / 32.0D;
            double d2 = (double) entity.serverPosZ / 32.0D;
            float f = packetIn.isRotating() ? (float) (packetIn.getYaw() * 360) / 256.0F : entity.rotationYaw;
            float f1 = packetIn.isRotating() ? (float) (packetIn.getPitch() * 360) / 256.0F : entity.rotationPitch;
            entity.setPositionAndRotation2(d0, d1, d2, f, f1, 3, false);
            entity.onGround = packetIn.getOnGround();
        }
    }

    public void handleEntityTeleport(S18PacketEntityTeleport packetIn) {
        Entity entity = mc.getNetHandler().clientWorldController.getEntityByID(packetIn.getEntityId());

        if (entity != null) {
            entity.serverPosX = packetIn.getX();
            entity.serverPosY = packetIn.getY();
            entity.serverPosZ = packetIn.getZ();
            double d0 = (double) entity.serverPosX / 32.0D;
            double d1 = (double) entity.serverPosY / 32.0D;
            double d2 = (double) entity.serverPosZ / 32.0D;
            float f = (float) (packetIn.getYaw() * 360) / 256.0F;
            float f1 = (float) (packetIn.getPitch() * 360) / 256.0F;

            if (Math.abs(entity.posX - d0) < 0.03125D && Math.abs(entity.posY - d1) < 0.015625D && Math.abs(entity.posZ - d2) < 0.03125D) {
                entity.setPositionAndRotation2(entity.posX, entity.posY, entity.posZ, f, f1, 3, true);
            } else {
                entity.setPositionAndRotation2(d0, d1, d2, f, f1, 3, true);
            }

            entity.onGround = packetIn.getOnGround();
        }
    }

    public void handleConfirmTransaction(S32PacketConfirmTransaction packetIn) {
        Container container = null;
        EntityPlayer entityplayer = mc.thePlayer;

        if (packetIn.getWindowId() == 0) {
            container = entityplayer.inventoryContainer;
        } else if (packetIn.getWindowId() == entityplayer.openContainer.windowId) {
            container = entityplayer.openContainer;
        }

        if (container != null && !packetIn.func_148888_e()) {
            mc.getNetHandler().addToSendQueue(new C0FPacketConfirmTransaction(packetIn.getWindowId(), packetIn.getActionNumber(), true));
        }
    }

    public boolean isDelaying() {
        return this.isEnabled() && !delayedPackets.isEmpty();
    }

    // --- ADDED METHOD ---
    private double getDistanceToPlayer(double x, double y, double z, float eyeHeight) {
        double dx = mc.thePlayer.posX - x;
        double dy = (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) - (y + eyeHeight);
        double dz = mc.thePlayer.posZ - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

}
