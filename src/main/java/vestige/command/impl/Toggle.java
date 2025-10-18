package vestige.command.impl;

import vestige.Vestige;
import vestige.command.Command;
import vestige.module.Module;
import vestige.util.misc.LogUtil;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class Toggle extends Command {

    public Toggle() {
        super("Toggle", "Turns on or off the specified module.", "t");
    }

    @Override
    public void onCommand(String[] args) {
        if (args.length >= 2) {
            Module module = Vestige.instance.getModuleManager().getModuleByNameNoSpace(args[1]);
            if (module != null) {
                module.toggle();
                sendToggleMessage(module.getName(), module.isEnabled());
            }
        }
    }

    public void sendToggleMessage(String moduleName, boolean enabled) {
        ChatComponentText prefix = new ChatComponentText("");
        ChatComponentText openBracket = new ChatComponentText("[");
        openBracket.getChatStyle().setColor(EnumChatFormatting.GRAY);

        ChatComponentText t = new ChatComponentText("Tellurium");
        t.getChatStyle().setColor(EnumChatFormatting.BLUE);

        ChatComponentText closeBracket = new ChatComponentText("]");
        closeBracket.getChatStyle().setColor(EnumChatFormatting.GRAY);

        ChatComponentText space = new ChatComponentText(" ");
        ChatComponentText msg = new ChatComponentText(moduleName + " toggled ");
        msg.getChatStyle().setColor(EnumChatFormatting.GRAY);

        ChatComponentText state = new ChatComponentText(enabled ? "ON" : "OFF");
        state.getChatStyle().setColor(enabled ? EnumChatFormatting.GREEN : EnumChatFormatting.RED);

        prefix.appendSibling(openBracket);
        prefix.appendSibling(t);
        prefix.appendSibling(closeBracket);
        prefix.appendSibling(space);
        prefix.appendSibling(msg);
        prefix.appendSibling(state);

        LogUtil.addChatMessage(prefix);
    }
}
