package vestige.command.impl;

import vestige.Vestige;
import vestige.command.Command;
import vestige.module.Module;
import vestige.setting.AbstractSetting;
import vestige.util.IMinecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class ModuleCommand extends Command implements IMinecraft {

    public ModuleCommand() {
        super("", "Lists or configures module settings by name.", "");
    }

    @Override
    public void onCommand(String[] args) {
        if (args.length >= 1) {
            String moduleNameInput = args[0];
            Module module = Vestige.instance.getModuleManager().getModuleByNameNoSpace(moduleNameInput);

            if (module != null) {
                if (args.length == 1) {
                    sendMessage(module.getName() + " settings:");
                    for (AbstractSetting setting : module.getSettings()) {
                        sendMessage(setting.getName());
                    }
                } else if (args.length >= 2) {
                    String settingName = args[1];
                    String value = args.length >= 3 ? args[2] : "";
                    AbstractSetting setting = module.getSettingByName(settingName);
                    if (setting != null) {
                        sendMessage(setting.getName() + " = " + value);
                    } else {
                        sendMessage("Setting not found: " + settingName);
                    }
                }
            } else {
                sendMessage("Module not found: " + moduleNameInput);
            }
        }
    }

    private void sendMessage(String msg) {
        ChatComponentText prefix = new ChatComponentText("");
        ChatComponentText openBracket = new ChatComponentText("[");
        openBracket.getChatStyle().setColor(EnumChatFormatting.GRAY);

        ChatComponentText t = new ChatComponentText("Tellurium");
        t.getChatStyle().setColor(EnumChatFormatting.BLUE);

        ChatComponentText closeBracket = new ChatComponentText("]");
        closeBracket.getChatStyle().setColor(EnumChatFormatting.GRAY);

        ChatComponentText space = new ChatComponentText(" ");
        ChatComponentText message = new ChatComponentText(msg);
        message.getChatStyle().setColor(EnumChatFormatting.GRAY);

        prefix.appendSibling(openBracket);
        prefix.appendSibling(t);
        prefix.appendSibling(closeBracket);
        prefix.appendSibling(space);
        prefix.appendSibling(message);

        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(prefix);
        }
    }
}
