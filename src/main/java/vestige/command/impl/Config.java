package vestige.command.impl;

import vestige.Vestige;
import vestige.command.Command;
import vestige.util.misc.LogUtil;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.File;

public class Config extends Command {

    public Config() {
        super("Config", "Loads or saves a config.");
    }

    @Override
    public void onCommand(String[] args) {
        if (args.length >= 2) {
            String action = args[1].toLowerCase();

            switch (action) {
                case "list":
                    File configDir = Vestige.instance.getFileSystem().getConfigFolder();
                    if (configDir.exists() && configDir.isDirectory()) {
                        String[] configs = configDir.list((dir, name) -> name.endsWith(".json"));
                        if (configs != null && configs.length > 0) {
                            sendPrefixMessage("Configs:");
                            for (String cfg : configs) {
                                sendPrefixMessage(" - " + cfg.replace(".json", ""));
                            }
                        } else {
                            sendPrefixMessage("No configs found.");
                        }
                    } else {
                        sendPrefixMessage("Config folder not found.");
                    }
                    break;

                case "folder":
                    try {
                        File folder = Vestige.instance.getFileSystem().getConfigFolder();
                        if (!folder.exists()) folder.mkdirs();
                        java.awt.Desktop.getDesktop().open(folder);
                        sendPrefixMessage("Opened configs folder.");
                    } catch (Exception e) {
                        sendPrefixMessage("Failed to open folder.");
                    }
                    break;

                case "load":
                case "save":
                    if (args.length < 3) {
                        sendPrefixMessage("Please specify a config name.");
                        return;
                    }
                    String configName = args[2];
                    if (action.equals("load")) {
                        boolean success = Vestige.instance.getFileSystem().loadConfig(configName, false);
                        if (success) sendPrefixMessage("Loaded config " + configName);
                        else sendPrefixMessage("Config not found.");
                    } else {
                        Vestige.instance.getFileSystem().saveConfig(configName);
                        sendPrefixMessage("Saved config " + configName);
                    }
                    break;

                default:
                    sendPrefixMessage("Unknown action. Use load, save, list, or folder.");
                    break;
            }
        } else {
            sendPrefixMessage("Usage: .config <load/save/list/folder> [name]");
        }
    }

    private void sendPrefixMessage(String message) {
        ChatComponentText prefix = new ChatComponentText("");
        ChatComponentText openBracket = new ChatComponentText("[");
        openBracket.getChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
        ChatComponentText t = new ChatComponentText("Tellurium");
        t.getChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
        ChatComponentText closeBracket = new ChatComponentText("]");
        closeBracket.getChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
        ChatComponentText msg = new ChatComponentText(" " + message);
        msg.getChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
        prefix.appendSibling(openBracket);
        prefix.appendSibling(t);
        prefix.appendSibling(closeBracket);
        prefix.appendSibling(msg);
        LogUtil.addChatMessage(prefix);
    }
}
