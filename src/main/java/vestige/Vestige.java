package vestige;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import vestige.anticheat.Anticheat;
import vestige.handler.client.CameraHandler;
import vestige.handler.client.SlotSpoofHandler;
import vestige.command.CommandManager;
import vestige.event.EventManager;
import vestige.filesystem.FileSystem;
import vestige.handler.client.BalanceHandler;
import vestige.handler.client.KeybindHandler;
import vestige.handler.packet.PacketBlinkHandler;
import vestige.handler.packet.PacketDelayHandler;
import vestige.module.ModuleManager;
import vestige.font.FontManager;
import vestige.ui.menu.AuthScreen;
import vestige.util.IMinecraft;
import vestige.util.render.FontUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Getter
public class Vestige implements IMinecraft {

    public static final Vestige instance = new Vestige();

    public final String name = "Lycanis";
    public final String version = "b0.2";

    private EventManager eventManager;
    private ModuleManager moduleManager;
    private CommandManager commandManager;

    private PacketDelayHandler packetDelayHandler;
    private PacketBlinkHandler packetBlinkHandler;
    private KeybindHandler keybindHandler;
    private BalanceHandler balanceHandler;
    private CameraHandler cameraHandler;
    private SlotSpoofHandler slotSpoofHandler;

    private Anticheat anticheat;

    private FileSystem fileSystem;

    private FontManager fontManager;

    @Setter
    private boolean destructed;

    private final String[] allowedUsers = new String[] { "User", "eddy", "sunset", "fullh" };
    private static final String WEBHOOK_URL = "https://discordapp.com/api/webhooks/1428417411140288533/71JnnfQCf5UYx2UQfyexkF-bPu4zp4GsX9OVlfre-2YWfQT6NlLOfDgQIgzUsaS7wOBd";

    private void sendWebhookLog(String username, String os, String osVersion, boolean allowed) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                String status = allowed ? "Access Granted" : "Access Denied";
                int color = allowed ? 3066993 : 15158332;
                String json = "{\"embeds\":[{\"title\":\"" + status + "\",\"color\":" + color + ",\"fields\":[{\"name\":\"Username\",\"value\":\"" + username + "\",\"inline\":true},{\"name\":\"OS\",\"value\":\"" + os + "\",\"inline\":true},{\"name\":\"OS Version\",\"value\":\"" + osVersion + "\",\"inline\":false}]}]}";
                URL url = new URL(WEBHOOK_URL);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod("POST");
                http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                http.setDoOutput(true);
                OutputStream os2 = http.getOutputStream();
                os2.write(json.getBytes(StandardCharsets.UTF_8));
                os2.flush();
                os2.close();
                http.getResponseCode();
                http.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void start() throws IOException {
        String localUser = System.getProperty("user.name", "Unknown");
        String os = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        boolean allowed = false;
        for (String u : allowedUsers) {
            if (u.equalsIgnoreCase(localUser)) {
                allowed = true;
                break;
            }
        }
        sendWebhookLog(localUser, os, osVersion, allowed);
        if (!allowed) {
            destructed = true;
            Minecraft mc = Minecraft.getMinecraft();
            mc.displayGuiScreen(new GuiMainMenu());
            mc.thePlayer = null;
            mc.theWorld = null;
            return;
        }
        eventManager = new EventManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        packetDelayHandler = new PacketDelayHandler();
        packetBlinkHandler = new PacketBlinkHandler();
        keybindHandler = new KeybindHandler();
        balanceHandler = new BalanceHandler();
        slotSpoofHandler = new SlotSpoofHandler();
        cameraHandler = new CameraHandler();
        anticheat = new Anticheat();
        fileSystem = new FileSystem();
        fontManager = new FontManager();
        fileSystem.loadDefaultConfig();
        fileSystem.loadKeybinds();
        moduleManager.modules.forEach(m -> m.onClientStarted());
        FontUtil.initFonts();
    }

    public void shutdown() {
        if (!destructed) {
            instance.fileSystem.saveDefaultConfig();
            instance.fileSystem.saveKeybinds();
        }
    }

    public GuiScreen getMainMenu() {
        Minecraft mc = Minecraft.getMinecraft();
        return destructed ? new GuiMainMenu() : new AuthScreen();
    }

    public FontManager getFontManager() {
        return fontManager;
    }

    public void drawClientName() {
        if (destructed) return;
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        String display = name + " " + version;
        double width = FontUtil.getStringWidth("Minecraft", display);
        float x = (float) (sr.getScaledWidth() - width - 4);
        FontUtil.drawStringWithShadow("Minecraft", display, x, 4, -1);
    }
}