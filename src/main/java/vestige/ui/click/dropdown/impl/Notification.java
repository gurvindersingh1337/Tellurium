package vestige.ui.click.dropdown.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import vestige.module.Module;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Notification {

    private static final List<Notification> notifications = new ArrayList<>();
    private static final Minecraft mc = Minecraft.getMinecraft();

    private String message;
    private long startTime;
    private long duration;
    private float x, y;

    public Notification(String message, long duration) {
        this.message = message;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        notifications.add(this);
    }

    public static void sendModuleToggle(Module module) {
        String state = module.isEnabled() ? "enabled" : "disabled";
        new Notification(module.getName() + " " + state, 2000);
    }

    public static void render() {
        ScaledResolution sr = new ScaledResolution(mc);
        float yOffset = sr.getScaledHeight() - 50;

        List<Notification> toRemove = new ArrayList<>();

        for(Notification notif : notifications) {
            long elapsed = System.currentTimeMillis() - notif.startTime;

            if(elapsed > notif.duration) {
                toRemove.add(notif);
                continue;
            }

            float progress = (float) elapsed / notif.duration;
            float alpha = progress < 0.1f ? progress * 10 : progress > 0.9f ? (1 - progress) * 10 : 1;

            int width = mc.fontRendererObj.getStringWidth(notif.message) + 20;
            int height = 30;

            notif.x = 10;
            notif.y = yOffset;

            drawRect(notif.x, notif.y, notif.x + width, notif.y + height, new Color(0, 0, 0, (int)(150 * alpha)).getRGB());
            drawRect(notif.x, notif.y, notif.x + 3, notif.y + height, new Color(255, 255, 255, (int)(255 * alpha)).getRGB());

            mc.fontRendererObj.drawStringWithShadow(notif.message, notif.x + 10, notif.y + 10, new Color(255, 255, 255, (int)(255 * alpha)).getRGB());

            yOffset -= height + 5;
        }

        notifications.removeAll(toRemove);
    }

    private static void drawRect(float left, float top, float right, float bottom, int color) {
        net.minecraft.client.gui.Gui.drawRect((int)left, (int)top, (int)right, (int)bottom, color);
    }
}