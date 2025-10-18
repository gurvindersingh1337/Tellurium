package vestige.command;

import vestige.Vestige;
import vestige.command.impl.*;
import vestige.event.Listener;
import vestige.event.impl.ChatSendEvent;
import vestige.util.misc.LogUtil;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;

public class CommandManager {

    public final ArrayList<Command> commands = new ArrayList<>();

    public CommandManager() {
        Vestige.instance.getEventManager().register(this);
        commands.add(new Toggle());
        commands.add(new Bind());
        commands.add(new Config());
        commands.add(new ModuleCommand());
    }

    @Listener
    public void onChatSend(ChatSendEvent event) {
        String message = event.getMessage();
        if (message.startsWith(".")) {
            event.setCancelled(true);
            StringBuilder commandName = new StringBuilder();
            for (int i = 1; i < message.length(); i++) {
                char c = message.charAt(i);
                if (c == ' ') break;
                commandName.append(c);
            }
            Command command = getCommandByName(commandName.toString());
            if (command != null) {
                String commandWithoutDot = message.substring(1);
                String[] commandParts = commandWithoutDot.split(" ");
                command.onCommand(commandParts);
            } else {
                ModuleCommand moduleCommand = (ModuleCommand) getCommandByName("");
                if(moduleCommand != null) {
                    String commandWithoutDot = message.substring(1);
                    String[] commandParts = commandWithoutDot.split(" ");
                    moduleCommand.onCommand(commandParts);
                } else {
                    sendMessage("Command not found.");
                }
            }
        }
    }

    public <T extends Command> T getCommandByName(String name) {
        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                return (T) command;
            } else if (command.getAliases() != null && command.getAliases().length > 0) {
                for (String alias : command.getAliases()) {
                    if (alias.equalsIgnoreCase(name)) {
                        return (T) command;
                    }
                }
            }
        }
        return null;
    }

    public static void sendMessage(String message) {
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
