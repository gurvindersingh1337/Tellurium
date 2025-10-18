package vestige.util.misc;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import vestige.Vestige;
import vestige.util.IMinecraft;

public class LogUtil implements IMinecraft {

    private static final String prefix = "[" + Vestige.instance.name + "]";

    public static void print(Object message) {
        System.out.println(prefix + " " + message);
    }

    public static void addChatMessage(IChatComponent component) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(component);
        }
    }

    public static void addChatMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
