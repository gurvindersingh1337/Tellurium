package vestige.command.impl;

import org.lwjgl.input.Keyboard;
import vestige.Vestige;
import vestige.command.Command;
import vestige.module.Module;
import vestige.util.misc.LogUtil;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class Bind extends Command {

    public Bind() {
        super("Bind", "Changes the keybind of the specified module.");
    }

    @Override
    public void onCommand(String[] args) {
        if (args.length >= 3) {
            Module module = Vestige.instance.getModuleManager().getModuleByNameNoSpace(args[1]);
            if (module != null) {
                String keyName = args[2].toUpperCase();
                module.setKey(Keyboard.getKeyIndex(keyName));
                sendPrefixMessage("Bound " + module.getName() + " to " + keyName);
            }
        }
    }

    private void sendPrefixMessage(String message) {
        ChatComponentText prefix = new ChatComponentText("");
        ChatComponentText openBracket = new ChatComponentText("[");
        openBracket.getChatStyle().setColor(EnumChatFormatting.GRAY);
        ChatComponentText t = new ChatComponentText("Tellurium");
        t.getChatStyle().setColor(EnumChatFormatting.BLUE);
        ChatComponentText closeBracket = new ChatComponentText("]");
        closeBracket.getChatStyle().setColor(EnumChatFormatting.GRAY);
        ChatComponentText msg = new ChatComponentText(" " + message);
        msg.getChatStyle().setColor(EnumChatFormatting.WHITE);
        prefix.appendSibling(openBracket);
        prefix.appendSibling(t);
        prefix.appendSibling(closeBracket);
        prefix.appendSibling(msg);
        LogUtil.addChatMessage(prefix);
    }
}
