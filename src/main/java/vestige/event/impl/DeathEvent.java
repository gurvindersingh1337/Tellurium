package vestige.event.impl;

import net.minecraft.entity.player.EntityPlayer;

public class DeathEvent {

    private final EntityPlayer victim;
    private final EntityPlayer killer;

    public DeathEvent(EntityPlayer victim, EntityPlayer killer) {
        this.victim = victim;
        this.killer = killer;
    }

    public EntityPlayer getVictim() {
        return victim;
    }

    public EntityPlayer getKiller() {
        return killer;
    }
}
